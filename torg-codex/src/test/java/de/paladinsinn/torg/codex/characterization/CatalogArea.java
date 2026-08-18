/*
 * Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
 *
 * Kaiserpfalz EDV-Service
 * Roland T. Lichti
 * Darmstädter Str. 12
 * 64625 Bensheim
 * GERMANY
 */

package de.paladinsinn.torg.codex.characterization;

import de.paladinsinn.torg.codex.data.repository.ArticleRepository;
import de.paladinsinn.torg.codex.data.repository.CosmRepository;
import de.paladinsinn.torg.codex.data.repository.ItemRepository;
import de.paladinsinn.torg.codex.data.repository.MiracleListRepository;
import de.paladinsinn.torg.codex.data.repository.MiracleRepository;
import de.paladinsinn.torg.codex.data.repository.PerkGroupRepository;
import de.paladinsinn.torg.codex.data.repository.PerkRepository;
import de.paladinsinn.torg.codex.data.repository.PowerListRepository;
import de.paladinsinn.torg.codex.data.repository.PowerRepository;
import de.paladinsinn.torg.codex.data.repository.PublicationRepository;
import de.paladinsinn.torg.codex.data.repository.RaceRepository;
import de.paladinsinn.torg.codex.data.repository.ShardRepository;
import de.paladinsinn.torg.codex.data.repository.SpellListRepository;
import de.paladinsinn.torg.codex.data.repository.SpellRepository;
import de.paladinsinn.torg.codex.data.repository.TagRepository;
import de.paladinsinn.torg.codex.data.repository.ThreatRepository;
import de.paladinsinn.torg.codex.data.repository.VehicleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@SuppressWarnings("rawtypes")
enum CatalogArea {
    ARTICLES("articles", "/api/articles", ArticleRepository.class, false),
    COSMS("cosms", "/api/cosms", CosmRepository.class, false),
    ITEMS("items", "/api/items", ItemRepository.class, true),
    MIRACLES("miracles", "/api/miracles", MiracleRepository.class, false),
    MIRACLE_LISTS("miracle-lists", "/api/miracle-lists", MiracleListRepository.class, true),
    PERKS("perks", "/api/perks", PerkRepository.class, true),
    PERK_GROUPS("perk-groups", "/api/perk-groups", PerkGroupRepository.class, false),
    POWERS("powers", "/api/powers", PowerRepository.class, false),
    POWER_LISTS("power-lists", "/api/power-lists", PowerListRepository.class, true),
    PUBLICATIONS("publications", "/api/publications", PublicationRepository.class, false),
    RACES("races", "/api/races", RaceRepository.class, false),
    SHARDS("shards", "/api/shards", ShardRepository.class, true),
    SPELLS("spells", "/api/spells", SpellRepository.class, false),
    SPELL_LISTS("spell-lists", "/api/spell-lists", SpellListRepository.class, true),
    TAGS("tags", "/api/tags", TagRepository.class, false),
    THREATS("threats", "/api/threats", ThreatRepository.class, true),
    VEHICLES("vehicles", "/api/vehicles", VehicleRepository.class, true);

    private final String fixtureDirectory;
    private final String collectionPath;
    private final Class<? extends JpaRepository> repositoryType;
    private final boolean supportsCosm;

    CatalogArea(
            String fixtureDirectory,
            String collectionPath,
            Class<? extends JpaRepository> repositoryType,
            boolean supportsCosm) {
        this.fixtureDirectory = fixtureDirectory;
        this.collectionPath = collectionPath;
        this.repositoryType = repositoryType;
        this.supportsCosm = supportsCosm;
    }

    String fixtureDirectory() {
        return fixtureDirectory;
    }

    String collectionPath() {
        return collectionPath;
    }

    Class<? extends JpaRepository> repositoryType() {
        return repositoryType;
    }

    boolean supportsCosm() {
        return supportsCosm;
    }
}
