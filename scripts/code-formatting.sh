#!/usr/bin/env bash

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

temp_base_dir="${TMPDIR:-${TEMP:-${TMP:-/tmp}}}"

target_java_files=()
if [ $# -gt 0 ]; then
    for file in "$@"; do
        [[ -f "$file" && "$file" == *.java ]] && target_java_files+=("$file")
    done
else
    while IFS= read -r file; do
        target_java_files+=("$file")
    done < <(git diff --cached --name-only --diff-filter=ACMR -- '*.java')
fi

[ ${#target_java_files[@]} -eq 0 ] && echo "[건너뜀] 포맷할 파일 없음." && exit 0

to_native_path() {
    command -v cygpath >/dev/null 2>&1 && cygpath -w "$1" || printf '%s\n' "$1"
}

find_formatter() {
    local -a windows_patterns=(
        "/c/Program Files/JetBrains/IntelliJ IDEA*/bin/format.bat"
        "/c/Program Files/JetBrains/IntelliJ IDEA Community Edition*/bin/format.bat"
    )
    for pattern in "${windows_patterns[@]}"; do
        while IFS= read -r match; do
            [ -n "$match" ] && [ -f "$match" ] && echo "$match" && return 0
        done < <(compgen -G "$pattern" || true)
    done
    return 1
}

formatter_bin="$(find_formatter)" || {
    echo "format.bat을 찾지 못했습니다."
    exit 1
}

absolute_files=()
for file in "${target_java_files[@]}"; do
    [ -f "$file" ] && absolute_files+=("$repo_root/$file")
done

run_formatter() {
    local temp_root
    temp_root="$(mktemp -d "$temp_base_dir/idea-format.XXXXXX")"
    
    local temp_config_win="$(to_native_path "$temp_root/config")"
    local temp_system_win="$(to_native_path "$temp_root/system")"
    local temp_log_win="$(to_native_path "$temp_root/log")"
    
    mkdir -p "$temp_root/config" "$temp_root/system" "$temp_root/log"

    local temp_properties="$temp_root/idea.properties"
    cat > "$temp_properties" <<EOF
idea.config.path=$temp_config_win
idea.system.path=$temp_system_win
idea.log.path=$temp_log_win
EOF

    export IDEA_PROPERTIES="$(to_native_path "$temp_properties")"

    local -a args=("-allowDefaults")
    for file in "${absolute_files[@]}"; do
        args+=("$(to_native_path "$file")")
    done

    echo "▶ 실행 중: $formatter_bin (대상: ${#absolute_files[@]}개)"
    
    if cmd.exe /c "$(to_native_path "$formatter_bin")" "${args[@]}" < /dev/null; then
        rm -rf "$temp_root"
        return 0
    else
        rm -rf "$temp_root"
        return 1
    fi
}

if ! run_formatter; then
    echo "------------------------------------------------"
    echo "[에러] 포맷터 실행 실패"
    echo "------------------------------------------------"
    exit 1
fi

git add -- "${target_java_files[@]}"
echo "[완료] 코드 스타일 포맷팅 성공!"