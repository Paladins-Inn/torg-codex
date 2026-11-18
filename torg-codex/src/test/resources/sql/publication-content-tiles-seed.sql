-- Idempotent seed for feature 009 (Publication Content Tiles).
-- Every row inserted by this script uses an id starting with 'e9c00000-'.

DELETE FROM torg_item_products WHERE CAST(item_id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_perk_products WHERE CAST(perk_id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_spell_products WHERE CAST(spell_id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_item WHERE CAST(id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_perk WHERE CAST(id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_spell WHERE CAST(id AS text) LIKE 'e9c00000-%';
DELETE FROM torg_publication WHERE CAST(id AS text) LIKE 'e9c00000-%';

-- Publications:
--   pct-paid-a: storefront product id > 0 (has thumbnail URLs)
--   pct-paid-b: primary_product_id = 0 (placeholder only)
--   pct-empty-pub: known publication with no accessible entries
--   core-rulebook: inserted only if not already present in the shared test database
INSERT INTO torg_publication (id, codex_id, name, primary_product_id)
VALUES ('e9c00000-0001-4000-8000-000000000001', 'pct-paid-a', 'PCT Paid Sourcebook A', 287803);

INSERT INTO torg_publication (id, codex_id, name, primary_product_id)
VALUES ('e9c00000-0001-4000-8000-000000000002', 'pct-paid-b', 'PCT Paid Sourcebook B', 0);

INSERT INTO torg_publication (id, codex_id, name, primary_product_id)
VALUES ('e9c00000-0001-4000-8000-000000000003', 'pct-empty-pub', 'PCT Empty Publication', 0);

INSERT INTO torg_publication (id, codex_id, name, primary_product_id)
SELECT 'e9c00000-0001-4000-8000-000000000004', 'core-rulebook', 'Core Rulebook', 205937
WHERE NOT EXISTS (
    SELECT 1 FROM torg_publication WHERE codex_id = 'core-rulebook'
);

-- Spells:
--   1: pct-paid-a only
--   2: shared between pct-paid-a and pct-paid-b
--   3: core-rulebook (free tier)
--   4: unassigned (no product)
--   5: unrelated product
INSERT INTO torg_spell (id, name, text)
VALUES ('e9c00000-000b-4000-8000-000000000001', 'PCT Paid A Only Spell', 'Spell only in Paid A.');
INSERT INTO torg_spell_products (spell_id, product)
VALUES ('e9c00000-000b-4000-8000-000000000001', 'pct-paid-a');

INSERT INTO torg_spell (id, name, text)
VALUES ('e9c00000-000b-4000-8000-000000000002', 'PCT Shared Spell', 'Spell shared by Paid A and Paid B.');
INSERT INTO torg_spell_products (spell_id, product)
VALUES ('e9c00000-000b-4000-8000-000000000002', 'pct-paid-a');
INSERT INTO torg_spell_products (spell_id, product)
VALUES ('e9c00000-000b-4000-8000-000000000002', 'pct-paid-b');

INSERT INTO torg_spell (id, name, text)
VALUES ('e9c00000-000b-4000-8000-000000000003', 'PCT Free Core Spell', 'Free-tier spell in Core Rulebook.');
INSERT INTO torg_spell_products (spell_id, product)
VALUES ('e9c00000-000b-4000-8000-000000000003', 'core-rulebook');

INSERT INTO torg_spell (id, name, text)
VALUES ('e9c00000-000b-4000-8000-000000000004', 'PCT Unassigned Spell', 'Unassigned spell.');

INSERT INTO torg_spell (id, name, text)
VALUES ('e9c00000-000b-4000-8000-000000000005', 'PCT Unrelated Spell', 'Spell in an unrelated product.');
INSERT INTO torg_spell_products (spell_id, product)
VALUES ('e9c00000-000b-4000-8000-000000000005', 'pct-unrelated');

-- Perks: second free-tier category assigned to core-rulebook
INSERT INTO torg_perk (id, name, contradiction, text)
VALUES ('e9c00000-0005-4000-8000-000000000001', 'PCT Free Core Perk', false, 'Free-tier perk in Core Rulebook.');
INSERT INTO torg_perk_products (perk_id, product)
VALUES ('e9c00000-0005-4000-8000-000000000001', 'core-rulebook');

-- Items: second category in pct-paid-a (while other categories like miracle/vehicle have 0 matches)
INSERT INTO torg_item (id, name, text)
VALUES ('e9c00000-0002-4000-8000-000000000001', 'PCT Paid A Item', 'Item only in Paid A.');
INSERT INTO torg_item_products (item_id, product)
VALUES ('e9c00000-0002-4000-8000-000000000001', 'pct-paid-a');
