-- Migrate phase (ADR-011): data-only correction for domain-level DRM (ADR-019, FR-018).
-- Idempotent; a no-op when the affected catalog rows are absent (public free-tier dataset).

-- 1. Assign the free tier to the core cosms that have no product assignment.
INSERT INTO torg_cosm_products (cosm_id, product)
SELECT c.id, 'core-rulebook'
FROM torg_cosm c
WHERE c.slug IN ('aysle', 'core-earth', 'cyberpapacy', 'living-land', 'nile-empire',
                 'orrorsh', 'pan-pacifica', 'tharkold', 'universal', 'unknown')
  AND NOT EXISTS (SELECT 1 FROM torg_cosm_products cp WHERE cp.cosm_id = c.id AND cp.product IS NOT NULL);

-- 2. Insert the products of each perk group's perks that the group is missing.
INSERT INTO torg_perk_group_products (group_id, product)
SELECT DISTINCT g.id, pp.product
FROM torg_perk_group g
JOIN torg_perk p ON p.perk_group = lower(replace(g.name, ' ', '-'))
JOIN torg_perk_products pp ON pp.perk_id = p.id
WHERE pp.product IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM torg_perk_group_products gp WHERE gp.group_id = g.id AND gp.product = pp.product);

-- 3. Remove every perk-group assignment outside the group's exact derived product union.
DELETE FROM torg_perk_group_products gp
WHERE NOT EXISTS (
    SELECT 1
    FROM torg_perk_group g
    JOIN torg_perk p ON p.perk_group = lower(replace(g.name, ' ', '-'))
    JOIN torg_perk_products pp ON pp.perk_id = p.id
    WHERE g.id = gp.group_id
      AND pp.product = gp.product);
