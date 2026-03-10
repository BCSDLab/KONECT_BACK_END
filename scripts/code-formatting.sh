#!/usr/bin/env bash

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

temp_base_dir="${TMPDIR:-${TEMP:-${TMP:-/tmp}}}"

target_java_files=()
while IFS= read -r file; do
    target_java_files+=("$file")
done < <(git diff --cached --name-only --diff-filter=ACMR -- '*.java')

if [ ${#target_java_files[@]} -eq 0 ]; then
    echo "[건너뜀] 포맷할 스테이징된 Java 파일이 없습니다."
    exit 0
fi

find_formatter() {
    local candidate
    local -a candidates=(
        "${IDEA_FORMATTER_BIN:-}"
        "${INTELLIJ_FORMATTER_BIN:-}"
        "/Applications/IntelliJ IDEA.app/Contents/MacOS/idea"
        "/Applications/IntelliJ IDEA Ultimate.app/Contents/MacOS/idea"
        "/Applications/IntelliJ IDEA CE.app/Contents/MacOS/idea"
        "/Applications/IntelliJ IDEA Community Edition.app/Contents/MacOS/idea"
        "/Applications/Android Studio.app/Contents/MacOS/studio"
        "/Applications/IntelliJ IDEA.app/Contents/bin/format.sh"
        "/Applications/IntelliJ IDEA Ultimate.app/Contents/bin/format.sh"
        "/Applications/IntelliJ IDEA CE.app/Contents/bin/format.sh"
        "/Applications/IntelliJ IDEA Community Edition.app/Contents/bin/format.sh"
        "/Applications/Android Studio.app/Contents/bin/format.sh"
        "idea"
        "studio"
        "idea64.exe"
        "idea.exe"
        "studio64.exe"
        "studio.exe"
        "format.sh"
    )
    local -a windows_patterns=(
        "/c/Program Files/JetBrains/IntelliJ IDEA*/bin/idea64.exe"
        "/c/Program Files/JetBrains/IntelliJ IDEA*/bin/idea.exe"
        "/c/Program Files/JetBrains/IntelliJ IDEA Community Edition*/bin/idea64.exe"
        "/c/Program Files/JetBrains/IntelliJ IDEA Community Edition*/bin/idea.exe"
        "/c/Program Files/Android/Android Studio/bin/studio64.exe"
        "/c/Program Files/Android/Android Studio/bin/studio.exe"
    )
    local local_app_data
    local local_app_data_unix
    local pattern
    local match

    if command -v cygpath >/dev/null 2>&1; then
        local_app_data="${LOCALAPPDATA:-}"
        if [ -n "$local_app_data" ]; then
            local_app_data_unix="$(cygpath -u "$local_app_data" 2>/dev/null || true)"
            if [ -n "$local_app_data_unix" ]; then
                windows_patterns+=(
                    "$local_app_data_unix/Programs/IntelliJ IDEA*/bin/idea64.exe"
                    "$local_app_data_unix/Programs/IntelliJ IDEA*/bin/idea.exe"
                    "$local_app_data_unix/Programs/Android Studio/bin/studio64.exe"
                    "$local_app_data_unix/Programs/Android Studio/bin/studio.exe"
                )
            fi
        fi
    fi

    for pattern in "${windows_patterns[@]}"; do
        while IFS= read -r match; do
            if [ -n "$match" ]; then
                candidates+=("$match")
            fi
        done < <(compgen -G "$pattern" || true)
    done

    for candidate in "${candidates[@]}"; do
        if [ -z "$candidate" ]; then
            continue
        fi

        if [ -x "$candidate" ]; then
            printf '%s\n' "$candidate"
            return 0
        fi

        if command -v "$candidate" >/dev/null 2>&1; then
            command -v "$candidate"
            return 0
        fi
    done

    return 1
}

find_code_style() {
    local candidate
    local -a candidates=(
        "$repo_root/.idea/codeStyles/Project.xml"
        "$repo_root/.idea/codeStyleSettings.xml"
        "$repo_root/config/intellij-code-style.xml"
    )

    for candidate in "${candidates[@]}"; do
        if [ -f "$candidate" ]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    return 1
}

formatter_bin="$(find_formatter)" || {
    echo "IntelliJ formatter를 찾지 못했습니다. IDEA_FORMATTER_BIN 또는 INTELLIJ_FORMATTER_BIN을 설정해 주세요."
    exit 1
}

style_file=""
if ! style_file="$(find_code_style)"; then
    style_file=""
    echo "프로젝트 IntelliJ 코드 스타일 파일이 없어 IntelliJ 기본 스타일을 사용합니다."
    echo "팀과 같은 스타일을 쓰려면 .idea/codeStyles/Project.xml 또는 config/intellij-code-style.xml을 추가해 주세요."
fi

absolute_files=()
for file in "${target_java_files[@]}"; do
    if [ -f "$file" ]; then
        absolute_files+=("$repo_root/$file")
    fi
done

if [ ${#absolute_files[@]} -eq 0 ]; then
    echo "[건너뜀] 실제로 존재하는 스테이징된 Java 파일이 없습니다."
    exit 0
fi

echo "IntelliJ formatter로 스테이징된 Java 파일 ${#absolute_files[@]}개를 포맷하는 중..."

is_launcher_formatter() {
    case "$1" in
        */MacOS/idea|*/MacOS/studio|idea|studio|*.exe)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

to_native_path() {
    local path="$1"
    local formatter="${2:-$formatter_bin}"
    if [[ "$formatter" == *.exe ]] && command -v cygpath >/dev/null 2>&1; then
        cygpath -w "$path"
        return 0
    fi
    printf '%s\n' "$path"
}

run_formatter() {
    local stdout_log
    local stderr_log
    stdout_log="$(mktemp "$temp_base_dir/idea-format-stdout.XXXXXX")"
    stderr_log="$(mktemp "$temp_base_dir/idea-format-stderr.XXXXXX")"
    local -a formatter_exec_args=()
    local -a formatter_file_args=()
    local file

    if [ -n "$style_file" ]; then
        formatter_exec_args+=("-s" "$(to_native_path "$style_file")")
    else
        formatter_exec_args+=("-allowDefaults")
    fi

    for file in "${absolute_files[@]}"; do
        formatter_file_args+=("$(to_native_path "$file")")
    done

    if is_launcher_formatter "$formatter_bin"; then
        local temp_root
        local temp_config
        local temp_system
        local temp_log

        temp_root="$(mktemp -d "$temp_base_dir/idea-format.XXXXXX")"
        mkdir -p "$temp_root/config" "$temp_root/system" "$temp_root/log"
        temp_config="$temp_root/config"
        temp_system="$temp_root/system"
        temp_log="$temp_root/log"

        temp_properties="$temp_root/idea.properties"
        cat > "$temp_properties" <<EOF
idea.config.path=$temp_config
idea.system.path=$temp_system
idea.log.path=$temp_log
EOF

        export IDEA_PROPERTIES="$temp_properties"
        if "$formatter_bin" format \
            "${formatter_exec_args[@]}" \
            "${formatter_file_args[@]}" \
            >"$stdout_log" \
            2>"$stderr_log"; then
            unset IDEA_PROPERTIES
            rm -rf "$temp_root"
            rm -f "$stdout_log" "$stderr_log"
            return 0
        fi
        unset IDEA_PROPERTIES

        cat "$stdout_log"
        cat "$stderr_log" >&2
        rm -rf "$temp_root"
        rm -f "$stdout_log" "$stderr_log"
        return 1
    fi

    if "$formatter_bin" format "${formatter_exec_args[@]}" "${formatter_file_args[@]}" >"$stdout_log" 2>"$stderr_log"; then
        rm -f "$stdout_log" "$stderr_log"
        return 0
    fi

    cat "$stdout_log"
    cat "$stderr_log" >&2
    rm -f "$stdout_log" "$stderr_log"
    return 1
}

if ! run_formatter; then
    echo "IntelliJ formatter 실행에 실패했습니다. IntelliJ IDEA가 열려 있다면 닫고 다시 시도해 주세요."
    exit 1
fi

git add -- "${target_java_files[@]}"

removed_from_stage=()
for file in "${target_java_files[@]}"; do
    if git diff --cached --quiet -- "$file"; then
        removed_from_stage+=("$file")
    fi
done

if [ ${#removed_from_stage[@]} -gt 0 ]; then
    echo "[안내] 포맷팅 후 스테이징 diff가 사라진 파일:"
    for file in "${removed_from_stage[@]}"; do
        echo "  - $file"
    done
fi

echo "[완료] 스테이징된 Java 파일에 IntelliJ 포맷을 적용했습니다."
