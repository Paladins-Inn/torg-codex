-- Removes every row inserted by publication-content-tiles-seed.sql (ids starting with 'e9c00000-').
-- Pre-existing rows (such as an already-loaded core-rulebook publication) are untouched.

DELETE FROM torg_item_products WHERE CAST(item_id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_perk_products WHERE CAST(perk_id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_spell_products WHERE CAST(spell_id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_item WHERE CAST(id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_perk WHERE CAST(id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_spell WHERE CAST(id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_publication WHERE CAST(id AS text) LIKE 'e9c00000-%';
