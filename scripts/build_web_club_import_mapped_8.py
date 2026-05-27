import csv
import re
from collections import Counter, OrderedDict
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[1]
WORKSPACE_ROOT = BACKEND_ROOT.parent
SOURCE_CSV = WORKSPACE_ROOT / "대학동아리_전국_재크롤링_v25_소개정제_mapped_2.csv"
MAPPED_CSV = WORKSPACE_ROOT / "대학동아리_전국_재크롤링_v25_소개정제_mapped_8.csv"
WEB_UNIVERSITY_CSV = BACKEND_ROOT / "scripts" / "generated" / "web_university_import.csv"
OUTPUT_DIR = BACKEND_ROOT / "scripts" / "generated"
IMPORT_CSV = OUTPUT_DIR / "web_club_import_mapped_8.csv"
IMPORT_SQL = OUTPUT_DIR / "web_club_import_mapped_8.sql"
ROLLBACK_SQL = OUTPUT_DIR / "web_club_import_mapped_8_rollback.sql"
REPORT = OUTPUT_DIR / "web_club_import_mapped_8_report.md"

SAMPLE_IMAGE_URL = "https://stage-static.koreatech.in/konect/university/university_logo_sample.webp"
DEFAULT_LOCATION = "미확인"
DEFAULT_TOPIC = "기타"
BACKUP_UNIVERSITY = "web_university_backup_before_mapped_8"
BACKUP_CLUB = "web_club_backup_before_mapped_8"

VALID_CATEGORIES = {
    "PERFORMANCE",
    "SOCIAL_SERVICE",
    "EXHIBITION_CREATION",
    "RELIGION",
    "SPORTS",
    "HOBBY",
    "ACADEMIC",
    "ETC",
}
CATEGORY_ORDER = [
    "PERFORMANCE",
    "SOCIAL_SERVICE",
    "EXHIBITION_CREATION",
    "RELIGION",
    "SPORTS",
    "HOBBY",
    "ACADEMIC",
    "ETC",
]
CATEGORY_NAMES = {
    "PERFORMANCE": "공연",
    "SOCIAL_SERVICE": "사회/봉사",
    "EXHIBITION_CREATION": "전시/창작",
    "RELIGION": "종교",
    "SPORTS": "체육(운동)",
    "HOBBY": "취미",
    "ACADEMIC": "학술",
    "ETC": "기타",
}
LEGACY_FALLBACK = {
    "PERFORMANCE": "PERFORMANCE",
    "RELIGION": "RELIGION",
    "SPORTS": "SPORTS",
    "HOBBY": "HOBBY",
    "ACADEMIC": "ACADEMIC",
    "JUNIOR": "ETC",
}
ALIASES = {
    "국립금오공대": "국립금오공과대학교",
    "장로회신대": "장로회신학대학교",
}


def clean(value):
    value = (value or "").strip()
    return re.sub(r"\s+", " ", value)


def truncate(value, limit):
    value = clean(value)
    return value if len(value) <= limit else value[:limit]


def sql(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def sql_row(values):
    parts = []
    for value in values:
        parts.append(str(value) if isinstance(value, int) else sql(value))
    return "(" + ", ".join(parts) + ")"


def compact_text(*values):
    return " ".join(clean(value) for value in values if clean(value)).lower()


def has_any(text, keywords):
    return any(keyword.lower() in text for keyword in keywords)


def classify_category(row):
    legacy_category = clean(row["분과"])
    text = compact_text(
        row["동아리 이름"],
        row["분과"],
        row["주제"],
        row["한줄소개"],
        row["상세소개"],
        row["보강내역"],
    )

    religion_keywords = [
        "종교", "기독", "천주교", "가톨릭", "카톨릭", "불교", "원불교", "선교", "성경", "예수",
        "교회", "ccc", "ivf", "sfc", "ubf", "esf", "dfc", "catholic", "bible", "불회", "네비게이토",
    ]
    sports_keywords = [
        "체육", "운동", "스포츠", "축구", "농구", "야구", "배구", "탁구", "테니스", "배드민턴",
        "볼링", "당구", "풋살", "러닝", "수영", "스키", "보드", "산악", "등산", "클라이밍",
        "검도", "유도", "태권", "무예", "무도", "헬스", "요가", "필라테스", "승마", "자전거",
        "골프", "라켓", "족구", "핸드볼", "미식축구", "e스포츠", "esports",
        "럭비", "복싱", "빙상", "사격", "양궁", "역도", "요트", "육상", "조정", "체조", "펜싱", "하키",
    ]
    social_service_keywords = [
        "봉사", "사회", "복지", "적십자", "rcy", "로타랙트", "해비타트", "굿네이버스", "유니세프",
        "멘토링", "교육봉사", "재능기부", "연탄", "헌혈", "의료봉사", "농활", "다문화", "장애",
        "지역사회", "캠페인", "환경보호", "동물보호", "보육원", "요양원", "취약계층", "기부",
    ]
    performance_keywords = [
        "공연", "음악", "밴드", "댄스", "연극", "노래", "합창", "오케스트라", "관현악", "통기타",
        "보컬", "힙합", "랩", "락", "록", "클래식", "국악", "풍물", "사물놀이", "응원단", "치어",
        "연행", "뮤지컬", "아카펠라", "합주", "기타반", "무용",
    ]
    exhibition_creation_keywords = [
        "전시", "창작", "미술", "사진", "서예", "캘리", "문예", "문학", "시집", "시화", "소설", "만화",
        "디자인", "공예", "영상", "영화", "애니", "일러스트", "그림", "출판", "회지", "웹툰",
        "촬영", "드로잉",
    ]
    academic_keywords = [
        "학술", "연구", "스터디", "it", "프로그래밍", "코딩", "컴퓨터", "로봇", "공학", "과학",
        "수학", "토론", "경제", "경영", "창업", "투자", "금융", "법", "정치", "언론", "방송/언론",
        "어학", "영어", "중국어", "일본어", "철학", "역사", "심리", "마케팅", "세미나",
        "기계", "자동차", "cad", "cae", "해킹", "정보보호", "소프트웨어", "전자", "전기", "화학",
        "생물", "생명", "건축", "취업", "임용", "고시", "공부", "프로젝트", "개발", "특허",
    ]
    hobby_keywords = [
        "취미", "문화", "교양", "여행", "게임", "보드게임", "요리", "커피", "바둑", "독서",
        "친목", "레저", "맛집", "다도", "차", "와인", "보드", "퍼즐", "캠핑", "꽃꽂이",
    ]

    if has_any(text, religion_keywords):
        return "RELIGION"
    if has_any(text, sports_keywords):
        return "SPORTS"
    if has_any(text, social_service_keywords):
        return "SOCIAL_SERVICE"
    if has_any(text, performance_keywords):
        return "PERFORMANCE"
    if has_any(text, academic_keywords):
        return "ACADEMIC"
    if has_any(text, exhibition_creation_keywords):
        return "EXHIBITION_CREATION"
    if has_any(text, hobby_keywords):
        return "HOBBY"

    return LEGACY_FALLBACK.get(legacy_category, "ETC")


def norm(value):
    return "".join(ch for ch in value.lower() if ch.isalnum())


def campus_from_source_name(name):
    match = re.search(r"\(([^)]+)\)", name)
    if not match:
        return "MAIN"
    campus = match.group(1).upper()
    return "MAIN" if campus in {"서울", "MAIN"} else "BRANCH"


def base_source_name(name):
    return re.sub(r"\([^)]*\)", "", name).strip()


def official_abbrevs(full_name):
    values = {full_name}
    if full_name.endswith("대학교"):
        values.add(full_name.removesuffix("대학교") + "대")
    if full_name.endswith("교육대학교"):
        values.add(full_name.removesuffix("교육대학교") + "교대")
    if full_name.endswith("여자대학교"):
        values.add(full_name.removesuffix("여자대학교") + "여대")
    return values


def build_university_lookup():
    rows = list(csv.DictReader(WEB_UNIVERSITY_CSV.open("r", encoding="utf-8-sig", newline="")))
    lookup = {}
    for row in rows:
        for key in official_abbrevs(row["korean_name"]):
            lookup[(norm(key), row["campus"])] = row
            lookup.setdefault((norm(key), None), row)
    return lookup


def resolve_university(source_name, lookup):
    base = base_source_name(source_name)
    campus = campus_from_source_name(source_name)
    candidates = [ALIASES.get(base, base)]
    if base.endswith("대"):
        candidates.append(base.removesuffix("대") + "대학교")
    if base.endswith("교대"):
        candidates.append(base.removesuffix("교대") + "교육대학교")

    for candidate in candidates:
        found = lookup.get((norm(candidate), campus)) or lookup.get((norm(candidate), None))
        if found:
            return found
    raise ValueError(f"Cannot resolve university: {source_name}")


def chunked(items, size):
    for index in range(0, len(items), size):
        yield items[index:index + size]


def read_source_rows():
    return list(csv.DictReader(SOURCE_CSV.open("r", encoding="utf-8-sig", newline="")))


def write_mapped_csv(rows):
    with MAPPED_CSV.open("w", encoding="utf-8-sig", newline="") as fp:
        writer = csv.DictWriter(fp, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(rows)


def write_import_csv(rows, source_to_university):
    with IMPORT_CSV.open("w", encoding="utf-8-sig", newline="") as fp:
        writer = csv.writer(fp)
        writer.writerow([
            "id",
            "university_id",
            "club_category",
            "name",
            "topic",
            "description",
            "introduce",
            "image_url",
            "location",
        ])
        for index, row in enumerate(rows, start=1):
            writer.writerow(build_club_record(index, row, source_to_university))


def build_club_record(index, row, source_to_university):
    university = source_to_university[row["정규화된 대학명"]]
    name = truncate(row["동아리 이름"], 50)
    topic = truncate(row["주제"] or DEFAULT_TOPIC, 20)
    description = clean(row["한줄소개"])
    if not description:
        description = f"{topic} 활동 동아리"
    description = truncate(description, 30)
    introduce = clean(row["상세소개"]) or clean(row["한줄소개"])
    if not introduce:
        introduce = f"{name}은(는) {topic} 활동을 하는 동아리입니다."
    return [
        index,
        int(university["id"]),
        row["분과"],
        name,
        topic,
        description,
        introduce,
        SAMPLE_IMAGE_URL,
        DEFAULT_LOCATION,
    ]


def write_sql(rows, universities, source_to_university):
    university_values = [
        sql_row([int(uni["id"]), uni["korean_name"], uni["campus"], uni["region"], uni["image_url"]])
        for _, uni in sorted(universities.items())
    ]
    club_values = [
        sql_row(build_club_record(index, row, source_to_university))
        for index, row in enumerate(rows, start=1)
    ]

    lines = [
        "-- Generated by scripts/build_web_club_import_mapped_8.py",
        "-- Target schema: web_university + web_club after V76/V78.",
        "-- To test without persisting, change the final COMMIT to ROLLBACK before running.",
        "",
        f"DROP TABLE IF EXISTS {BACKUP_CLUB};",
        f"CREATE TABLE {BACKUP_CLUB} LIKE web_club;",
        f"INSERT INTO {BACKUP_CLUB} SELECT * FROM web_club;",
        f"DROP TABLE IF EXISTS {BACKUP_UNIVERSITY};",
        f"CREATE TABLE {BACKUP_UNIVERSITY} LIKE web_university;",
        f"INSERT INTO {BACKUP_UNIVERSITY} SELECT * FROM web_university;",
        "",
        "START TRANSACTION;",
        "",
        "DELETE FROM web_club;",
        "DELETE FROM web_university;",
        "",
        "INSERT INTO web_university (id, korean_name, campus, region, image_url) VALUES",
        ",\n".join(university_values) + ";",
        "",
    ]
    for batch_number, chunk in enumerate(chunked(club_values, 500), start=1):
        lines.extend([
            f"-- web_club batch {batch_number}",
            "INSERT INTO web_club (id, university_id, club_category, name, topic, description, introduce, image_url, location) VALUES",
            ",\n".join(chunk) + ";",
            "",
        ])
    lines.extend([
        "SELECT",
        "    (SELECT COUNT(*) FROM web_university) AS imported_web_university_count,",
        "    (SELECT COUNT(*) FROM web_club) AS imported_web_club_count;",
        "",
        "COMMIT;",
        "",
        f"-- Rollback after COMMIT is available via {ROLLBACK_SQL.name}, as long as the backup tables remain.",
    ])
    IMPORT_SQL.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_rollback_sql():
    lines = [
        "-- Restores web_university and web_club from backup tables created by web_club_import_mapped_8.sql.",
        "-- Run this only after confirming the backup tables exist and contain the desired previous state.",
        "",
        "START TRANSACTION;",
        "",
        "DELETE FROM web_club;",
        "DELETE FROM web_university;",
        "",
        f"INSERT INTO web_university SELECT * FROM {BACKUP_UNIVERSITY};",
        f"INSERT INTO web_club SELECT * FROM {BACKUP_CLUB};",
        "",
        "SELECT",
        "    (SELECT COUNT(*) FROM web_university) AS restored_web_university_count,",
        "    (SELECT COUNT(*) FROM web_club) AS restored_web_club_count;",
        "",
        "COMMIT;",
    ]
    ROLLBACK_SQL.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_report(rows, source_to_university, universities, category_counts, transition_counts):
    lines = [
        "# Web Club Import mapped_8 Report",
        "",
        f"- Source CSV: `{SOURCE_CSV.name}`",
        f"- Mapped CSV: `{MAPPED_CSV.name}`",
        f"- Import CSV: `{IMPORT_CSV.name}`",
        f"- Import SQL: `{IMPORT_SQL.name}`",
        f"- Rollback SQL: `{ROLLBACK_SQL.name}`",
        f"- Source rows: {len(rows):,}",
        f"- Distinct source university labels: {len(source_to_university):,}",
        f"- Generated web_university rows: {len(universities):,}",
        f"- Generated web_club rows: {len(rows):,}",
        "",
        "## Category Counts",
        "",
    ]
    for key in CATEGORY_ORDER:
        lines.append(f"- {key} ({CATEGORY_NAMES[key]}): {category_counts[key]:,}")

    lines.extend(["", "## Legacy Category Transitions", ""])
    for (legacy, category), count in sorted(transition_counts.items()):
        lines.append(f"- {legacy} -> {category}: {count:,}")

    lines.extend([
        "",
        "## Backup Tables",
        "",
        f"- `{BACKUP_UNIVERSITY}`",
        f"- `{BACKUP_CLUB}`",
    ])
    REPORT.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    rows = read_source_rows()
    lookup = build_university_lookup()
    source_to_university = OrderedDict()
    category_counts = Counter()
    transition_counts = Counter()

    for row in rows:
        source_name = row["정규화된 대학명"]
        if source_name not in source_to_university:
            source_to_university[source_name] = resolve_university(source_name, lookup)

        legacy_category = clean(row["분과"])
        category = classify_category(row)
        if category not in VALID_CATEGORIES:
            raise ValueError(f"Invalid category: {category}")
        row["분과"] = category
        category_counts[category] += 1
        transition_counts[(legacy_category, category)] += 1

    universities = OrderedDict()
    for university in source_to_university.values():
        universities[int(university["id"])] = university

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    write_mapped_csv(rows)
    write_import_csv(rows, source_to_university)
    write_sql(rows, universities, source_to_university)
    write_rollback_sql()
    write_report(rows, source_to_university, universities, category_counts, transition_counts)

    print(f"Mapped CSV: {MAPPED_CSV}")
    print(f"Import CSV: {IMPORT_CSV}")
    print(f"Import SQL: {IMPORT_SQL}")
    print(f"Rollback SQL: {ROLLBACK_SQL}")
    print(f"Report: {REPORT}")
    print(f"Rows: {len(rows):,}")
    print(dict(category_counts))


if __name__ == "__main__":
    main()
