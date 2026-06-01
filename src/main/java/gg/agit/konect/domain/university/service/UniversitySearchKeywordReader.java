package gg.agit.konect.domain.university.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.university.model.UniversitySearchKeyword;
import gg.agit.konect.domain.university.repository.UniversitySearchKeywordRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversitySearchKeywordReader {

    private final UniversitySearchKeywordRepository universitySearchKeywordRepository;

    public Map<String, List<String>> getKeywordsByUniversityName(Collection<String> universityNames) {
        if (universityNames.isEmpty()) {
            return Map.of();
        }

        return universitySearchKeywordRepository.findAllByUniversityNames(universityNames)
            .stream()
            .collect(Collectors.groupingBy(
                keyword -> keyword.getUniversity().getKoreanName(),
                Collectors.mapping(UniversitySearchKeyword::getKeyword, Collectors.toList())
            ));
    }
}
