package gg.agit.konect.admin.version.repository;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.version.model.Version;

public interface AdminVersionRepository extends Repository<Version, Integer> {

    void save(Version version);
}
