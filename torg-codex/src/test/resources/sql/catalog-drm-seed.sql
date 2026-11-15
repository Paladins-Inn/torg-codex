-- Start from a clean state (idempotent seed).

DELETE FROM torg_cosm_products WHERE CAST(cosm_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_item_products WHERE CAST(item_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_item_cosms WHERE CAST(item_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_miracle_products WHERE CAST(miracle_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_miracle_list_products WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_miracle_list_cosms WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_perk_products WHERE CAST(perk_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_perk_cosms WHERE CAST(perk_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_perk_group_products WHERE CAST(group_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_power_products WHERE CAST(power_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_power_list_products WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_power_list_cosms WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_race_products WHERE CAST(race_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_shard_products WHERE CAST(shard_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_shard_cosms WHERE CAST(shard_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_spell_products WHERE CAST(spell_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_spell_list_products WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_spell_list_cosms WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_threat_products WHERE CAST(threat_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_threat_cosms WHERE CAST(threat_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_vehicle_products WHERE CAST(vehicle_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_vehicle_cosms WHERE CAST(vehicle_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_spell_list_entries WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_miracle_list_entries WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_power_list_entries WHERE CAST(list_id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_cosm WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_item WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_miracle WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_miracle_list WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_perk WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_perk_group WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_power WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_power_list WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_race WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_shard WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_spell WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_spell_list WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_threat WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_vehicle WHERE CAST(id AS text) LIKE 'd7a00000-%';
DELETE FROM torg_article WHERE CAST(id AS text) LIKE 'd7a00000-%';

-- Deterministic DRM seed for CatalogDrmDifferentialTest (feature 005, ADR-019).
-- Every row uses an id starting with 'd7a00000-'; catalog-drm-cleanup.sql removes them again.
-- Variants per area: 1 core-rulebook, 2 sourcebook-aysle + drm-third-product, 3 drm-third-product only, 4 no product.
-- Independent of the proprietary catalog CSVs.

-- cosm
INSERT INTO torg_cosm (id, name, slug, text) VALUES ('d7a00000-0001-4000-8000-000000000001', 'DRM-cosm-Core', 'drm-cosm', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_cosm_products (cosm_id, product) VALUES ('d7a00000-0001-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_cosm (id, name, slug, text) VALUES ('d7a00000-0001-4000-8000-000000000002', 'DRM-cosm-Aysle', 'drm-cosm-aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_cosm_products (cosm_id, product) VALUES ('d7a00000-0001-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_cosm_products (cosm_id, product) VALUES ('d7a00000-0001-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_cosm (id, name, slug, text) VALUES ('d7a00000-0001-4000-8000-000000000003', 'DRM-cosm-Third', 'drm-cosm-third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_cosm_products (cosm_id, product) VALUES ('d7a00000-0001-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_cosm (id, name, slug, text) VALUES ('d7a00000-0001-4000-8000-000000000004', 'DRM-cosm-Unassigned', 'drm-cosm-unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');

-- item
INSERT INTO torg_item (id, name, text) VALUES ('d7a00000-0002-4000-8000-000000000001', 'DRM-item-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_item_products (item_id, product) VALUES ('d7a00000-0002-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_item_cosms (item_id, cosm) VALUES ('d7a00000-0002-4000-8000-000000000001', 'drm-cosm');
INSERT INTO torg_item (id, name, text) VALUES ('d7a00000-0002-4000-8000-000000000002', 'DRM-item-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_item_products (item_id, product) VALUES ('d7a00000-0002-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_item_products (item_id, product) VALUES ('d7a00000-0002-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_item_cosms (item_id, cosm) VALUES ('d7a00000-0002-4000-8000-000000000002', 'drm-cosm');
INSERT INTO torg_item (id, name, text) VALUES ('d7a00000-0002-4000-8000-000000000003', 'DRM-item-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_item_products (item_id, product) VALUES ('d7a00000-0002-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_item_cosms (item_id, cosm) VALUES ('d7a00000-0002-4000-8000-000000000003', 'drm-cosm');
INSERT INTO torg_item (id, name, text) VALUES ('d7a00000-0002-4000-8000-000000000004', 'DRM-item-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_item_cosms (item_id, cosm) VALUES ('d7a00000-0002-4000-8000-000000000004', 'drm-cosm');

-- miracle
INSERT INTO torg_miracle (id, name, text) VALUES ('d7a00000-0003-4000-8000-000000000001', 'DRM-miracle-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_miracle_products (miracle_id, product) VALUES ('d7a00000-0003-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_miracle (id, name, text) VALUES ('d7a00000-0003-4000-8000-000000000002', 'DRM-miracle-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_miracle_products (miracle_id, product) VALUES ('d7a00000-0003-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_miracle_products (miracle_id, product) VALUES ('d7a00000-0003-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_miracle (id, name, text) VALUES ('d7a00000-0003-4000-8000-000000000003', 'DRM-miracle-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_miracle_products (miracle_id, product) VALUES ('d7a00000-0003-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_miracle (id, name, text) VALUES ('d7a00000-0003-4000-8000-000000000004', 'DRM-miracle-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');

-- miracle-list
INSERT INTO torg_miracle_list (id, name, text) VALUES ('d7a00000-0004-4000-8000-000000000001', 'DRM-miracle-list-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_miracle_list_products (list_id, product) VALUES ('d7a00000-0004-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_miracle_list_cosms (list_id, cosm) VALUES ('d7a00000-0004-4000-8000-000000000001', 'drm-cosm');
INSERT INTO torg_miracle_list (id, name, text) VALUES ('d7a00000-0004-4000-8000-000000000002', 'DRM-miracle-list-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_miracle_list_products (list_id, product) VALUES ('d7a00000-0004-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_miracle_list_products (list_id, product) VALUES ('d7a00000-0004-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_miracle_list_cosms (list_id, cosm) VALUES ('d7a00000-0004-4000-8000-000000000002', 'drm-cosm');
INSERT INTO torg_miracle_list (id, name, text) VALUES ('d7a00000-0004-4000-8000-000000000003', 'DRM-miracle-list-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_miracle_list_products (list_id, product) VALUES ('d7a00000-0004-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_miracle_list_cosms (list_id, cosm) VALUES ('d7a00000-0004-4000-8000-000000000003', 'drm-cosm');
INSERT INTO torg_miracle_list (id, name, text) VALUES ('d7a00000-0004-4000-8000-000000000004', 'DRM-miracle-list-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_miracle_list_cosms (list_id, cosm) VALUES ('d7a00000-0004-4000-8000-000000000004', 'drm-cosm');

-- perk
INSERT INTO torg_perk (id, name, contradiction, text) VALUES ('d7a00000-0005-4000-8000-000000000001', 'DRM-perk-Core', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_perk_products (perk_id, product) VALUES ('d7a00000-0005-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_perk_cosms (perk_id, cosm) VALUES ('d7a00000-0005-4000-8000-000000000001', 'drm-cosm');
INSERT INTO torg_perk (id, name, contradiction, text) VALUES ('d7a00000-0005-4000-8000-000000000002', 'DRM-perk-Aysle', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_perk_products (perk_id, product) VALUES ('d7a00000-0005-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_perk_products (perk_id, product) VALUES ('d7a00000-0005-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_perk_cosms (perk_id, cosm) VALUES ('d7a00000-0005-4000-8000-000000000002', 'drm-cosm');
INSERT INTO torg_perk (id, name, contradiction, text) VALUES ('d7a00000-0005-4000-8000-000000000003', 'DRM-perk-Third', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_perk_products (perk_id, product) VALUES ('d7a00000-0005-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_perk_cosms (perk_id, cosm) VALUES ('d7a00000-0005-4000-8000-000000000003', 'drm-cosm');
INSERT INTO torg_perk (id, name, contradiction, text) VALUES ('d7a00000-0005-4000-8000-000000000004', 'DRM-perk-Unassigned', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_perk_cosms (perk_id, cosm) VALUES ('d7a00000-0005-4000-8000-000000000004', 'drm-cosm');

-- group
INSERT INTO torg_perk_group (id, name, text) VALUES ('d7a00000-0006-4000-8000-000000000001', 'DRM-group-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_perk_group_products (group_id, product) VALUES ('d7a00000-0006-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_perk_group (id, name, text) VALUES ('d7a00000-0006-4000-8000-000000000002', 'DRM-group-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_perk_group_products (group_id, product) VALUES ('d7a00000-0006-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_perk_group_products (group_id, product) VALUES ('d7a00000-0006-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_perk_group (id, name, text) VALUES ('d7a00000-0006-4000-8000-000000000003', 'DRM-group-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_perk_group_products (group_id, product) VALUES ('d7a00000-0006-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_perk_group (id, name, text) VALUES ('d7a00000-0006-4000-8000-000000000004', 'DRM-group-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');

-- power
INSERT INTO torg_power (id, name, text) VALUES ('d7a00000-0007-4000-8000-000000000001', 'DRM-power-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_power_products (power_id, product) VALUES ('d7a00000-0007-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_power (id, name, text) VALUES ('d7a00000-0007-4000-8000-000000000002', 'DRM-power-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_power_products (power_id, product) VALUES ('d7a00000-0007-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_power_products (power_id, product) VALUES ('d7a00000-0007-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_power (id, name, text) VALUES ('d7a00000-0007-4000-8000-000000000003', 'DRM-power-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_power_products (power_id, product) VALUES ('d7a00000-0007-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_power (id, name, text) VALUES ('d7a00000-0007-4000-8000-000000000004', 'DRM-power-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');

-- power-list
INSERT INTO torg_power_list (id, name, text) VALUES ('d7a00000-0008-4000-8000-000000000001', 'DRM-power-list-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_power_list_products (list_id, product) VALUES ('d7a00000-0008-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_power_list_cosms (list_id, cosm) VALUES ('d7a00000-0008-4000-8000-000000000001', 'drm-cosm');
INSERT INTO torg_power_list (id, name, text) VALUES ('d7a00000-0008-4000-8000-000000000002', 'DRM-power-list-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_power_list_products (list_id, product) VALUES ('d7a00000-0008-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_power_list_products (list_id, product) VALUES ('d7a00000-0008-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_power_list_cosms (list_id, cosm) VALUES ('d7a00000-0008-4000-8000-000000000002', 'drm-cosm');
INSERT INTO torg_power_list (id, name, text) VALUES ('d7a00000-0008-4000-8000-000000000003', 'DRM-power-list-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_power_list_products (list_id, product) VALUES ('d7a00000-0008-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_power_list_cosms (list_id, cosm) VALUES ('d7a00000-0008-4000-8000-000000000003', 'drm-cosm');
INSERT INTO torg_power_list (id, name, text) VALUES ('d7a00000-0008-4000-8000-000000000004', 'DRM-power-list-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_power_list_cosms (list_id, cosm) VALUES ('d7a00000-0008-4000-8000-000000000004', 'drm-cosm');

-- race
INSERT INTO torg_race (id, name, major, text) VALUES ('d7a00000-0009-4000-8000-000000000001', 'DRM-race-Core', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_race_products (race_id, product) VALUES ('d7a00000-0009-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_race (id, name, major, text) VALUES ('d7a00000-0009-4000-8000-000000000002', 'DRM-race-Aysle', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_race_products (race_id, product) VALUES ('d7a00000-0009-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_race_products (race_id, product) VALUES ('d7a00000-0009-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_race (id, name, major, text) VALUES ('d7a00000-0009-4000-8000-000000000003', 'DRM-race-Third', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_race_products (race_id, product) VALUES ('d7a00000-0009-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_race (id, name, major, text) VALUES ('d7a00000-0009-4000-8000-000000000004', 'DRM-race-Unassigned', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');

-- shard
INSERT INTO torg_shard (id, name, text) VALUES ('d7a00000-000a-4000-8000-000000000001', 'DRM-shard-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_shard_products (shard_id, product) VALUES ('d7a00000-000a-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_shard_cosms (shard_id, cosm) VALUES ('d7a00000-000a-4000-8000-000000000001', 'drm-cosm');
INSERT INTO torg_shard (id, name, text) VALUES ('d7a00000-000a-4000-8000-000000000002', 'DRM-shard-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_shard_products (shard_id, product) VALUES ('d7a00000-000a-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_shard_products (shard_id, product) VALUES ('d7a00000-000a-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_shard_cosms (shard_id, cosm) VALUES ('d7a00000-000a-4000-8000-000000000002', 'drm-cosm');
INSERT INTO torg_shard (id, name, text) VALUES ('d7a00000-000a-4000-8000-000000000003', 'DRM-shard-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_shard_products (shard_id, product) VALUES ('d7a00000-000a-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_shard_cosms (shard_id, cosm) VALUES ('d7a00000-000a-4000-8000-000000000003', 'drm-cosm');
INSERT INTO torg_shard (id, name, text) VALUES ('d7a00000-000a-4000-8000-000000000004', 'DRM-shard-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_shard_cosms (shard_id, cosm) VALUES ('d7a00000-000a-4000-8000-000000000004', 'drm-cosm');

-- spell
INSERT INTO torg_spell (id, name, text) VALUES ('d7a00000-000b-4000-8000-000000000001', 'DRM-spell-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_spell_products (spell_id, product) VALUES ('d7a00000-000b-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_spell (id, name, text) VALUES ('d7a00000-000b-4000-8000-000000000002', 'DRM-spell-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_spell_products (spell_id, product) VALUES ('d7a00000-000b-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_spell_products (spell_id, product) VALUES ('d7a00000-000b-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_spell (id, name, text) VALUES ('d7a00000-000b-4000-8000-000000000003', 'DRM-spell-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_spell_products (spell_id, product) VALUES ('d7a00000-000b-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_spell (id, name, text) VALUES ('d7a00000-000b-4000-8000-000000000004', 'DRM-spell-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');

-- spell-list
INSERT INTO torg_spell_list (id, name, text) VALUES ('d7a00000-000c-4000-8000-000000000001', 'DRM-spell-list-Core', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_spell_list_products (list_id, product) VALUES ('d7a00000-000c-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_spell_list_cosms (list_id, cosm) VALUES ('d7a00000-000c-4000-8000-000000000001', 'drm-cosm');
INSERT INTO torg_spell_list (id, name, text) VALUES ('d7a00000-000c-4000-8000-000000000002', 'DRM-spell-list-Aysle', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_spell_list_products (list_id, product) VALUES ('d7a00000-000c-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_spell_list_products (list_id, product) VALUES ('d7a00000-000c-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_spell_list_cosms (list_id, cosm) VALUES ('d7a00000-000c-4000-8000-000000000002', 'drm-cosm');
INSERT INTO torg_spell_list (id, name, text) VALUES ('d7a00000-000c-4000-8000-000000000003', 'DRM-spell-list-Third', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_spell_list_products (list_id, product) VALUES ('d7a00000-000c-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_spell_list_cosms (list_id, cosm) VALUES ('d7a00000-000c-4000-8000-000000000003', 'drm-cosm');
INSERT INTO torg_spell_list (id, name, text) VALUES ('d7a00000-000c-4000-8000-000000000004', 'DRM-spell-list-Unassigned', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_spell_list_cosms (list_id, cosm) VALUES ('d7a00000-000c-4000-8000-000000000004', 'drm-cosm');

-- threat
INSERT INTO torg_threat (id, name, "unique", text) VALUES ('d7a00000-000d-4000-8000-000000000001', 'DRM-threat-Core', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_threat_products (threat_id, product) VALUES ('d7a00000-000d-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_threat_cosms (threat_id, cosm) VALUES ('d7a00000-000d-4000-8000-000000000001', 'drm-cosm');
INSERT INTO torg_threat (id, name, "unique", text) VALUES ('d7a00000-000d-4000-8000-000000000002', 'DRM-threat-Aysle', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_threat_products (threat_id, product) VALUES ('d7a00000-000d-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_threat_products (threat_id, product) VALUES ('d7a00000-000d-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_threat_cosms (threat_id, cosm) VALUES ('d7a00000-000d-4000-8000-000000000002', 'drm-cosm');
INSERT INTO torg_threat (id, name, "unique", text) VALUES ('d7a00000-000d-4000-8000-000000000003', 'DRM-threat-Third', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_threat_products (threat_id, product) VALUES ('d7a00000-000d-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_threat_cosms (threat_id, cosm) VALUES ('d7a00000-000d-4000-8000-000000000003', 'drm-cosm');
INSERT INTO torg_threat (id, name, "unique", text) VALUES ('d7a00000-000d-4000-8000-000000000004', 'DRM-threat-Unassigned', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_threat_cosms (threat_id, cosm) VALUES ('d7a00000-000d-4000-8000-000000000004', 'drm-cosm');

-- vehicle
INSERT INTO torg_vehicle (id, name, "unique", text) VALUES ('d7a00000-000e-4000-8000-000000000001', 'DRM-vehicle-Core', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_vehicle_products (vehicle_id, product) VALUES ('d7a00000-000e-4000-8000-000000000001', 'core-rulebook');
INSERT INTO torg_vehicle_cosms (vehicle_id, cosm) VALUES ('d7a00000-000e-4000-8000-000000000001', 'drm-cosm');
INSERT INTO torg_vehicle (id, name, "unique", text) VALUES ('d7a00000-000e-4000-8000-000000000002', 'DRM-vehicle-Aysle', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_vehicle_products (vehicle_id, product) VALUES ('d7a00000-000e-4000-8000-000000000002', 'sourcebook-aysle');
INSERT INTO torg_vehicle_products (vehicle_id, product) VALUES ('d7a00000-000e-4000-8000-000000000002', 'drm-third-product');
INSERT INTO torg_vehicle_cosms (vehicle_id, cosm) VALUES ('d7a00000-000e-4000-8000-000000000002', 'drm-cosm');
INSERT INTO torg_vehicle (id, name, "unique", text) VALUES ('d7a00000-000e-4000-8000-000000000003', 'DRM-vehicle-Third', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_vehicle_products (vehicle_id, product) VALUES ('d7a00000-000e-4000-8000-000000000003', 'drm-third-product');
INSERT INTO torg_vehicle_cosms (vehicle_id, cosm) VALUES ('d7a00000-000e-4000-8000-000000000003', 'drm-cosm');
INSERT INTO torg_vehicle (id, name, "unique", text) VALUES ('d7a00000-000e-4000-8000-000000000004', 'DRM-vehicle-Unassigned', false, 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_vehicle_cosms (vehicle_id, cosm) VALUES ('d7a00000-000e-4000-8000-000000000004', 'drm-cosm');

-- spell-list entries (children of area spell; order: aysle, core, missing, unassigned, third)
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000001', 'd7a00000-000b-4000-8000-000000000002', 0);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000001', 'd7a00000-000b-4000-8000-000000000001', 1);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000001', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000001', 'd7a00000-000b-4000-8000-000000000004', 3);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000001', 'd7a00000-000b-4000-8000-000000000003', 4);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000002', 'd7a00000-000b-4000-8000-000000000002', 0);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000002', 'd7a00000-000b-4000-8000-000000000001', 1);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000002', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000002', 'd7a00000-000b-4000-8000-000000000004', 3);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000002', 'd7a00000-000b-4000-8000-000000000003', 4);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000003', 'd7a00000-000b-4000-8000-000000000002', 0);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000003', 'd7a00000-000b-4000-8000-000000000001', 1);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000003', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000003', 'd7a00000-000b-4000-8000-000000000004', 3);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000003', 'd7a00000-000b-4000-8000-000000000003', 4);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000004', 'd7a00000-000b-4000-8000-000000000002', 0);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000004', 'd7a00000-000b-4000-8000-000000000001', 1);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000004', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000004', 'd7a00000-000b-4000-8000-000000000004', 3);
INSERT INTO torg_spell_list_entries (list_id, spell_id, entry_order) VALUES ('d7a00000-000c-4000-8000-000000000004', 'd7a00000-000b-4000-8000-000000000003', 4);

-- miracle-list entries (children of area miracle; order: aysle, core, missing, unassigned, third)
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000001', 'd7a00000-0003-4000-8000-000000000002', 0);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000001', 'd7a00000-0003-4000-8000-000000000001', 1);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000001', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000001', 'd7a00000-0003-4000-8000-000000000004', 3);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000001', 'd7a00000-0003-4000-8000-000000000003', 4);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000002', 'd7a00000-0003-4000-8000-000000000002', 0);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000002', 'd7a00000-0003-4000-8000-000000000001', 1);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000002', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000002', 'd7a00000-0003-4000-8000-000000000004', 3);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000002', 'd7a00000-0003-4000-8000-000000000003', 4);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000003', 'd7a00000-0003-4000-8000-000000000002', 0);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000003', 'd7a00000-0003-4000-8000-000000000001', 1);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000003', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000003', 'd7a00000-0003-4000-8000-000000000004', 3);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000003', 'd7a00000-0003-4000-8000-000000000003', 4);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000004', 'd7a00000-0003-4000-8000-000000000002', 0);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000004', 'd7a00000-0003-4000-8000-000000000001', 1);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000004', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000004', 'd7a00000-0003-4000-8000-000000000004', 3);
INSERT INTO torg_miracle_list_entries (list_id, miracle_id, entry_order) VALUES ('d7a00000-0004-4000-8000-000000000004', 'd7a00000-0003-4000-8000-000000000003', 4);

-- power-list entries (children of area power; order: aysle, core, missing, unassigned, third)
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000001', 'd7a00000-0007-4000-8000-000000000002', 0);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000001', 'd7a00000-0007-4000-8000-000000000001', 1);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000001', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000001', 'd7a00000-0007-4000-8000-000000000004', 3);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000001', 'd7a00000-0007-4000-8000-000000000003', 4);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000002', 'd7a00000-0007-4000-8000-000000000002', 0);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000002', 'd7a00000-0007-4000-8000-000000000001', 1);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000002', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000002', 'd7a00000-0007-4000-8000-000000000004', 3);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000002', 'd7a00000-0007-4000-8000-000000000003', 4);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000003', 'd7a00000-0007-4000-8000-000000000002', 0);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000003', 'd7a00000-0007-4000-8000-000000000001', 1);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000003', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000003', 'd7a00000-0007-4000-8000-000000000004', 3);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000003', 'd7a00000-0007-4000-8000-000000000003', 4);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000004', 'd7a00000-0007-4000-8000-000000000002', 0);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000004', 'd7a00000-0007-4000-8000-000000000001', 1);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000004', 'd7a00000-00ff-4000-8000-000000000099', 2);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000004', 'd7a00000-0007-4000-8000-000000000004', 3);
INSERT INTO torg_power_list_entries (list_id, power_id, entry_order) VALUES ('d7a00000-0008-4000-8000-000000000004', 'd7a00000-0007-4000-8000-000000000003', 4);

-- core item referencing the unassigned cosm 'drm-cosm-unassigned' (cosm reference fallback)
INSERT INTO torg_item (id, name, text) VALUES ('d7a00000-0002-4000-8000-000000000005', 'DRM-item-UnassignedCosm', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
INSERT INTO torg_item_products (item_id, product) VALUES ('d7a00000-0002-4000-8000-000000000005', 'core-rulebook');
INSERT INTO torg_item_cosms (item_id, cosm) VALUES ('d7a00000-0002-4000-8000-000000000005', 'drm-cosm-unassigned');

-- public article with a core-gated passage (effective free tier, SC-007)
INSERT INTO torg_article (id, name, text) VALUES ('d7a00000-000f-4000-8000-000000000001', 'DRM-article', 'DRM entry text. <IF:core-rulebook>DRM-CORE-PASSAGE</IF><IF:!core-rulebook>DRM-UPSELL-PASSAGE</IF>');
