#!/usr/bin/env bash

set -euo pipefail

invocation_dir="$(pwd)"
repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

temp_base_dir="${TMPDIR:-${TEMP:-${TMP:-/tmp}}}"

target_java_files=()
target_mode="staged"
pre_format_snapshot=""

resolve_target_java_file() {
    local input_file="$1"
    local resolved_file=""
    local matched_files=""
    local match_count

    case "$input_file" in
        *.java) ;;
        *) return 1 ;;
    esac

    if [ -f "$input_file" ]; then
        resolved_file="$input_file"
    elif [ -f "$invocation_dir/$input_file" ]; then
        resolved_file="$invocation_dir/$input_file"
    elif [ -f "$repo_root/$input_file" ]; then
        resolved_file="$repo_root/$input_file"
    else
        matched_files="$(git ls-files -- "$input_file" "*/$input_file" 2>/dev/null || true)"
        if [ -z "$matched_files" ]; then
            return 1
        fi

        match_count="$(printf '%s\n' "$matched_files" | wc -l | tr -d ' ')"
        if [ "$match_count" -gt 1 ]; then
            echo "[건너뜀] 여러 Java 파일이 일치해 파일명을 특정할 수 없습니다: $input_file" >&2
            printf '%s\n' "$matched_files" >&2
            return 1
        fi

        resolved_file="$repo_root/$matched_files"
    fi

    case "$resolved_file" in
        "$repo_root"/*)
            printf '%s\n' "${resolved_file#$repo_root/}"
            return 0
            ;;
    esac

    echo "[건너뜀] 레포 외부 파일은 포맷할 수 없습니다: $input_file" >&2
    return 1
}

# 파일 인자가 있으면 인자 사용, 없으면 스테이징된 파일 사용
if [ $# -gt 0 ]; then
    target_mode="explicit"
    for file in "$@"; do
        resolved_file="$(resolve_target_java_file "$file" || true)"
        if [ -n "$resolved_file" ]; then
            target_java_files+=("$resolved_file")
        fi
    done
else
    staged_java_files="$(git diff --cached --name-only --diff-filter=ACMR -- '*.java')"
    if [ -n "$staged_java_files" ]; then
        while IFS= read -r file; do
            if [ -n "$file" ]; then
                target_java_files+=("$file")
            fi
        done <<EOF
$staged_java_files
EOF
    fi
fi

if [ ${#target_java_files[@]} -eq 0 ]; then
    echo "[건너뜀] 포맷할 Java 파일이 없습니다."
    exit 0
fi

if [ "$target_mode" = "explicit" ]; then
    pre_format_snapshot="$(git diff --no-ext-diff -- "${target_java_files[@]}" 2>/dev/null || true)"
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
        matched_paths="$(compgen -G "$pattern" || true)"
        if [ -n "$matched_paths" ]; then
            while IFS= read -r match; do
                if [ -n "$match" ]; then
                    candidates+=("$match")
                fi
            done <<EOF
$matched_paths
EOF
        fi
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

find_google_java_format() {
    local candidate
    local -a candidates=(
        "${GOOGLE_JAVA_FORMAT_BIN:-}"
        "$repo_root/tools/google-java-format/google-java-format.jar"
        "$repo_root/tools/google-java-format/google-java-format-all-deps.jar"
        "google-java-format"
    )

    for candidate in "${candidates[@]}"; do
        if [ -z "$candidate" ]; then
            continue
        fi

        if [ -f "$candidate" ]; then
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

download_google_java_format() {
    local version="1.35.0"
    local cache_root="${XDG_CACHE_HOME:-$HOME/.cache}/konect"
    local target_dir="$cache_root/google-java-format"
    local target_file="$target_dir/google-java-format-${version}-all-deps.jar"
    local download_url="https://repo1.maven.org/maven2/com/google/googlejavaformat/google-java-format/${version}/google-java-format-${version}-all-deps.jar"

    if [ -f "$target_file" ]; then
        printf '%s\n' "$target_file"
        return 0
    fi

    if ! command -v curl >/dev/null 2>&1; then
        return 1
    fi

    mkdir -p "$target_dir"
    curl --fail --location --silent --show-error "$download_url" --output "$target_file"
    printf '%s\n' "$target_file"
}

run_unused_import_cleanup() {
    local formatter
    local -a cleanup_args=()

    formatter="$(find_google_java_format || true)"
    if [ -z "$formatter" ]; then
        formatter="$(download_google_java_format || true)"
    fi

    if [ -z "$formatter" ]; then
        echo "google-java-format을 찾지 못해 미사용 import 제거를 건너뜁니다."
        echo "GOOGLE_JAVA_FORMAT_BIN을 설정하거나 curl 사용 가능한 환경에서 다시 실행해 주세요."
        return 1
    fi

    echo "미사용 import를 제거하는 중..."

    if [[ "$formatter" == *.jar ]]; then
        if ! command -v java >/dev/null 2>&1; then
            echo "Java 런타임이 없어 미사용 import를 제거할 수 없습니다."
            return 1
        fi

        cleanup_args=(java -jar "$formatter")
    else
        cleanup_args=("$formatter")
    fi

    "${cleanup_args[@]}" --replace --fix-imports-only --skip-sorting-imports "${absolute_files[@]}"
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

if [ "$target_mode" = "explicit" ]; then
    echo "IntelliJ formatter로 지정된 Java 파일 ${#absolute_files[@]}개를 포맷하는 중..."
else
    echo "IntelliJ formatter로 스테이징된 Java 파일 ${#absolute_files[@]}개를 포맷하는 중..."
fi
echo "[대상 파일]"
for file in "${target_java_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  - $file"
    fi
done

if ! run_unused_import_cleanup; then
    exit 1
fi

is_launcher_formatter() {
    local basename
    basename=$(basename "$1" .sh)  # .sh 확장자 제거
    case "$basename" in
        idea|idea64|studio|studio64)
            return 0
            ;;
        *.exe)
            # Windows exe 파일에서 이름 추출
            basename=$(basename "$1" .exe)
            case "$basename" in
                idea|idea64|studio|studio64)
                    return 0
                    ;;
            esac
            ;;
    esac
    return 1
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

    # Non-launcher (format.sh 등): 서브커맨드 없이 직접 호출
    if "$formatter_bin" "${formatter_exec_args[@]}" "${formatter_file_args[@]}" >"$stdout_log" 2>"$stderr_log"; then
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

if [ "$target_mode" = "staged" ]; then
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
else
    post_format_snapshot="$(git diff --no-ext-diff -- "${target_java_files[@]}" 2>/dev/null || true)"

    if [[ "$pre_format_snapshot" != "$post_format_snapshot" ]]; then
        echo "[완료] 지정된 Java 파일에 포맷과 미사용 import 정리를 적용했습니다."
    else
        echo "[완료] 지정된 Java 파일에 변경할 내용이 없습니다."
    fi
fi
