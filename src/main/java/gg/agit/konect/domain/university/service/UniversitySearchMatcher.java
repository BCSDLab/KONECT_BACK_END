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

    public boolean matches(University university, String query) {
        return matches(university.getKoreanName(), query, List.of());
    }

    public boolean matches(String universityName, String query) {
        return matches(universityName, query, List.of());
    }

    public boolean matches(String universityName, String query, List<String> managedKeywords) {
        if (!StringUtils.hasText(query)) {
            return true;
        }

        String normalizedQuery = normalize(query);
        return getSearchTokens(universityName, managedKeywords)
            .anyMatch(token -> token.contains(normalizedQuery));
    }

    private Stream<String> getSearchTokens(String universityName, List<String> managedKeywords) {
        Set<String> aliases = getDefaultAliases(universityName);
        aliases.addAll(managedKeywords);

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
