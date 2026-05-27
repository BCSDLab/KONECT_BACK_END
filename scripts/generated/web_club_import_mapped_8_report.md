# Web Club Import mapped_8 Report

- Source CSV: `대학동아리_전국_재크롤링_v25_소개정제_mapped_2.csv`
- Mapped CSV: `대학동아리_전국_재크롤링_v25_소개정제_mapped_8.csv`
- Import CSV: `web_club_import_mapped_8.csv`
- Import SQL: `web_club_import_mapped_8.sql`
- Rollback SQL: `web_club_import_mapped_8_rollback.sql`
- Source rows: 4,725
- Distinct source university labels: 155
- Generated web_university rows: 154
- Generated web_club rows: 4,725

## Category Counts

- PERFORMANCE (공연): 796
- SOCIAL_SERVICE (사회/봉사): 404
- EXHIBITION_CREATION (전시/창작): 344
- RELIGION (종교): 372
- SPORTS (체육(운동)): 837
- HOBBY (취미): 150
- ACADEMIC (학술): 600
- ETC (기타): 1,222

## Legacy Category Transitions

- ACADEMIC -> ACADEMIC: 369
- ACADEMIC -> PERFORMANCE: 32
- ACADEMIC -> RELIGION: 3
- ACADEMIC -> SOCIAL_SERVICE: 15
- ACADEMIC -> SPORTS: 13
- HOBBY -> ACADEMIC: 68
- HOBBY -> EXHIBITION_CREATION: 198
- HOBBY -> HOBBY: 61
- HOBBY -> PERFORMANCE: 38
- HOBBY -> RELIGION: 1
- HOBBY -> SOCIAL_SERVICE: 6
- HOBBY -> SPORTS: 24
- JUNIOR -> ACADEMIC: 124
- JUNIOR -> ETC: 1,222
- JUNIOR -> EXHIBITION_CREATION: 31
- JUNIOR -> HOBBY: 71
- JUNIOR -> PERFORMANCE: 132
- JUNIOR -> RELIGION: 73
- JUNIOR -> SOCIAL_SERVICE: 356
- JUNIOR -> SPORTS: 116
- PERFORMANCE -> ACADEMIC: 30
- PERFORMANCE -> EXHIBITION_CREATION: 105
- PERFORMANCE -> HOBBY: 12
- PERFORMANCE -> PERFORMANCE: 587
- PERFORMANCE -> RELIGION: 18
- PERFORMANCE -> SOCIAL_SERVICE: 26
- PERFORMANCE -> SPORTS: 50
- RELIGION -> ACADEMIC: 3
- RELIGION -> EXHIBITION_CREATION: 6
- RELIGION -> HOBBY: 2
- RELIGION -> PERFORMANCE: 1
- RELIGION -> RELIGION: 272
- RELIGION -> SPORTS: 1
- SPORTS -> ACADEMIC: 6
- SPORTS -> EXHIBITION_CREATION: 4
- SPORTS -> HOBBY: 4
- SPORTS -> PERFORMANCE: 6
- SPORTS -> RELIGION: 5
- SPORTS -> SOCIAL_SERVICE: 1
- SPORTS -> SPORTS: 633

## Backup Tables

- `web_university_backup_before_mapped_8`
- `web_club_backup_before_mapped_8`
