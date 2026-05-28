package gg.agit.konect.domain.university.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.university.model.University;

@Component
public class UniversitySearchMatcher {

    private static final int HANGUL_SYLLABLE_START = 0xAC00;
    private static final int HANGUL_SYLLABLE_END = 0xD7A3;
    private static final int HANGUL_SYLLABLE_INTERVAL = 588;
    private static final int UNIVERSITY_SUFFIX_LENGTH = "대학교".length();

    private static final List<String> CHOSEONG = List.of(
        "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
        "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    );

    private static final Map<String, String> COMPATIBILITY_JAMO_CLUSTERS = Map.ofEntries(
        Map.entry("ㄳ", "ㄱㅅ"),
        Map.entry("ㄵ", "ㄴㅈ"),
        Map.entry("ㄶ", "ㄴㅎ"),
        Map.entry("ㄺ", "ㄹㄱ"),
        Map.entry("ㄻ", "ㄹㅁ"),
        Map.entry("ㄼ", "ㄹㅂ"),
        Map.entry("ㄽ", "ㄹㅅ"),
        Map.entry("ㄾ", "ㄹㅌ"),
        Map.entry("ㄿ", "ㄹㅍ"),
        Map.entry("ㅀ", "ㄹㅎ"),
        Map.entry("ㅄ", "ㅂㅅ")
    );

    private static final Map<String, List<String>> UNIVERSITY_ALIASES = Map.ofEntries(
        Map.entry("가톨릭대학교", List.of("가대")),
        Map.entry("건국대학교", List.of("건대")),
        Map.entry("경북대학교", List.of("경대", "경북대")),
        Map.entry("경희대학교", List.of("경희대")),
        Map.entry("고려대학교", List.of("고대")),
        Map.entry("광주과학기술원", List.of("광주과기원", "지스트", "gist")),
        Map.entry("단국대학교", List.of("단대")),
        Map.entry("대구경북과학기술원", List.of("대경과기원", "디지스트", "dgist")),
        Map.entry("동국대학교", List.of("동대")),
        Map.entry("부산대학교", List.of("부대", "부산대")),
        Map.entry("서강대학교", List.of("서강대")),
        Map.entry("서울과학기술대학교", List.of("과기대", "서울과기대")),
        Map.entry("서울대학교", List.of("설대", "서울대")),
        Map.entry("서울시립대학교", List.of("시립대", "서울시립대")),
        Map.entry("성균관대학교", List.of("성대")),
        Map.entry("연세대학교", List.of("연대")),
        Map.entry("울산과학기술원", List.of("울산과기원", "유니스트", "unist")),
        Map.entry("육군사관학교", List.of("육사")),
        Map.entry("이화여자대학교", List.of("이대", "이화여대")),
        Map.entry("전남대학교", List.of("전대", "전남대")),
        Map.entry("중앙대학교", List.of("중대")),
        Map.entry("충남대학교", List.of("충대", "충남대")),
        Map.entry("충북대학교", List.of("충북대")),
        Map.entry("포항공과대학교", List.of("포공", "포스텍", "postech")),
        Map.entry("한국공학대학교", List.of("한공대", "한국공대")),
        Map.entry("한국과학기술원", List.of("카이스트", "kaist")),
        Map.entry("한국교통대학교", List.of("교통대", "한국교통대")),
        Map.entry("한국기술교육대학교", List.of("한기대", "코리아텍", "koreatech")),
        Map.entry("한국외국어대학교", List.of("외대", "한국외대")),
        Map.entry("한국체육대학교", List.of("한체대")),
        Map.entry("한국항공대학교", List.of("항공대", "한국항공대")),
        Map.entry("한국해양대학교", List.of("해양대", "한국해양대")),
        Map.entry("해군사관학교", List.of("해사")),
        Map.entry("홍익대학교", List.of("홍대"))
    );

    public boolean matches(University university, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }

        String normalizedQuery = normalize(query);
        return getSearchTokens(university.getKoreanName())
            .anyMatch(token -> token.contains(normalizedQuery));
    }

    private Stream<String> getSearchTokens(String universityName) {
        Set<String> aliases = getDefaultAliases(universityName);
        aliases.addAll(UNIVERSITY_ALIASES.getOrDefault(universityName, List.of()));

        List<String> normalizedAliases = aliases.stream()
            .map(this::normalize)
            .toList();

        return Stream.concat(
            normalizedAliases.stream(),
            normalizedAliases.stream().map(this::getChoseong)
        ).distinct();
    }

    private Set<String> getDefaultAliases(String universityName) {
        String withoutWhitespace = universityName.replaceAll("\\s", "");
        String withoutCampus = withoutWhitespace.replaceAll("(서울|세종|글로벌|ERICA|WISE)캠퍼스$", "");
        Set<String> aliases = new HashSet<>();

        aliases.add(withoutWhitespace);
        aliases.add(withoutCampus);

        if (withoutCampus.endsWith("대학교")) {
            aliases.add(withoutCampus.substring(0, withoutCampus.length() - UNIVERSITY_SUFFIX_LENGTH) + "대");
        }

        if (withoutCampus.startsWith("국립")) {
            String withoutNational = withoutCampus.substring(2);
            aliases.add(withoutNational);

            if (withoutNational.endsWith("대학교")) {
                aliases.add(withoutNational.substring(0, withoutNational.length() - UNIVERSITY_SUFFIX_LENGTH) + "대");
            }
        }

        return aliases;
    }

    private String normalize(String value) {
        return expandCompatibilityJamoClusters(value)
            .replaceAll("\\s", "")
            .toLowerCase();
    }

    private String expandCompatibilityJamoClusters(String value) {
        StringBuilder builder = new StringBuilder();

        value.codePoints()
            .mapToObj(Character::toString)
            .forEach(character -> builder.append(COMPATIBILITY_JAMO_CLUSTERS.getOrDefault(character, character)));

        return builder.toString();
    }

    private String getChoseong(String value) {
        StringBuilder builder = new StringBuilder();

        value.codePoints()
            .forEach(codePoint -> builder.append(toChoseong(codePoint)));

        return builder.toString();
    }

    private String toChoseong(int codePoint) {
        if (codePoint < HANGUL_SYLLABLE_START || codePoint > HANGUL_SYLLABLE_END) {
            return Character.toString(codePoint);
        }

        return CHOSEONG.get((codePoint - HANGUL_SYLLABLE_START) / HANGUL_SYLLABLE_INTERVAL);
    }
}
