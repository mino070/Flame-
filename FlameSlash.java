package com.flamekatana;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.GameMode;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Flame Slash: a crescent of fire that flies forward, burns and damages
 * everything it touches. At night it is wider, longer, hits harder and
 * recharges faster, and the flames turn to blue soul fire.
 */
public final class FlameSlash {

    /** The crescent is drawn from -ARC to +ARC degrees. */
    private static final double ARC_DEGREES = 70.0;
    /** The slash is tilted so it looks like a diagonal sword cut. */
    private static final double TILT_DEGREES = 30.0;

    private final FlameKatana plugin;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public FlameSlash(FlameKatana plugin) {
        this.plugin = plugin;
    }

    public void clearCooldown(UUID id) {
        lastUse.remove(id);
    }

    /** True when the player's world is in its night window. */
    public boolean isNight(World world) {
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        FileConfiguration cfg = plugin.getConfig();
        long start = cfg.getLong("night.start-tick", 13000);
        long end = cfg.getLong("night.end-tick", 23000);
        long time = world.getTime() % 24000;
        if (start <= end) {
            return time >= start && time < end;
        }
        return time >= start || time < end; // window wraps around midnight
    }

    public void cast(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        boolean night = isNight(player.getWorld());

        double cooldown = night ? cfg.getDouble("night.cooldown", 1.5) : cfg.getDouble("slash.cooldown", 3.0);
        double damage = cfg.getDouble("slash.damage", 8.0)
                * (night ? cfg.getDouble("night.damage-multiplier", 1.5) : 1.0);
        double range = night ? cfg.getDouble("night.range", 16.0) : cfg.getDouble("slash.range", 10.0);
        double radius = night ? cfg.getDouble("night.radius", 3.5) : cfg.getDouble("slash.radius", 2.5);
        int fireTicks = (int) ((night ? cfg.getDouble("night.fire-seconds", 8) : cfg.getDouble("slash.fire-seconds", 4)) * 20);
        double speed = Math.max(0.3, cfg.getDouble("slash.speed", 1.2));
        double knockback = cfg.getDouble("slash.knockback", 0.6);

        // ----- cooldown -----
        long now = System.currentTimeMillis();
        long ready = lastUse.getOrDefault(player.getUniqueId(), 0L) + (long) (cooldown * 1000);
        if (now < ready) {
            String left = String.format("%.1f", (ready - now) / 1000.0);
            String msg = cfg.getString("messages.cooldown", "Flame Slash ready in %time%s").replace("%time%", left);
            player.sendActionBar(Component.text(msg, NamedTextColor.RED));
            return;
        }
        lastUse.put(player.getUniqueId(), now);

        String msg = night
                ? cfg.getString("messages.slash-night", "Night Flame Slash! (power boosted)")
                : cfg.getString("messages.slash-day", "Flame Slash!");
        player.sendActionBar(Component.text(msg, night ? NamedTextColor.AQUA : NamedTextColor.GOLD));

        // ----- start effects -----
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        player.swingMainHand();
        world.playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, night ? 0.7f : 1.0f);
        world.playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1.0f, night ? 0.7f : 1.2f);
        if (night) {
            world.playSound(eye, Sound.ENTITY_WITHER_SHOOT, 0.4f, 1.6f);
        }

        // ----- build the slash's own coordinate system -----
        Vector dir = eye.getDirection().normalize();
        Vector right = dir.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0E-4) {          // looking straight up/down
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        Vector up = right.clone().crossProduct(dir).normalize();

        double tilt = Math.toRadians(TILT_DEGREES);
        // axis = the line the crescent is stretched along (tilted diagonal)
        Vector axis = right.clone().multiply(Math.cos(tilt)).add(up.clone().multiply(Math.sin(tilt))).normalize();
        // perp = thickness direction (sideways to the crescent's surface)
        Vector perp = axis.clone().crossProduct(dir).normalize();

        Location origin = eye.clone().add(0, -0.4, 0);
        final Particle flame = night ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;

        new BukkitRunnable() {
            double travelled = 1.0;
            final Set<UUID> alreadyHit = new HashSet<>();

            @Override
            public void run() {
                if (!player.isOnline() || travelled > range) {
                    cancel();
                    return;
                }

                Location center = origin.clone().add(dir.clone().multiply(travelled));

                // stop when the wave hits a wall
                if (!center.getBlock().isPassable()) {
                    impact(center, flame);
                    cancel();
                    return;
                }

                drawCrescent(center, dir, axis, radius, flame, night);
                damageTouching(player, center, dir, axis, perp, radius, damage, fireTicks, knockback, alreadyHit, flame);

                travelled += speed;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ------------------------------------------------------------------
    //  Particles
    // ------------------------------------------------------------------

    private void drawCrescent(Location center, Vector dir, Vector axis, double radius,
                              Particle flame, boolean night) {
        World world = center.getWorld();
        double step = 6.0; // degrees between particles

        for (double deg = -ARC_DEGREES; deg <= ARC_DEGREES; deg += step) {
            double a = Math.toRadians(deg);
            double lateral = radius * Math.sin(a);
            double forward = radius * Math.cos(a) - radius; // tips curve backwards

            // outer edge (bright) and a slightly thicker inner edge
            for (double thick : new double[]{0.0, 0.35}) {
                Location p = center.clone()
                        .add(axis.clone().multiply(lateral * (1.0 - thick * 0.15)))
                        .add(dir.clone().multiply(forward - thick));
                world.spawnParticle(flame, p, 2, 0.06, 0.06, 0.06, 0.005);
            }
        }

        // sparks / smoke following the blade
        Location mid = center.clone();
        world.spawnParticle(Particle.SMOKE, mid, 4, radius * 0.4, 0.2, radius * 0.4, 0.01);
        world.spawnParticle(Particle.LAVA, mid, night ? 3 : 1, radius * 0.4, 0.3, radius * 0.4, 0.0);
        if (night) {
            world.spawnParticle(Particle.SOUL, mid, 2, radius * 0.5, 0.3, radius * 0.5, 0.01);
        }
    }

    private void impact(Location where, Particle flame) {
        World world = where.getWorld();
        world.spawnParticle(flame, where, 40, 0.6, 0.6, 0.6, 0.08);
        world.spawnParticle(Particle.LAVA, where, 10, 0.5, 0.5, 0.5, 0.0);
        world.playSound(where, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.0f);
    }

    // ------------------------------------------------------------------
    //  Damage
    // ------------------------------------------------------------------

    private void damageTouching(Player owner, Location center, Vector dir, Vector axis, Vector perp,
                                double radius, double damage, int fireTicks, double knockback,
                                Set<UUID> alreadyHit, Particle flame) {
        World world = center.getWorld();
        double reach = radius + 2.0;
        Vector c = center.toVector();

        for (Entity e : world.getNearbyEntities(center, reach, reach, reach)) {
            if (!(e instanceof LivingEntity target)) continue;
            if (target.equals(owner) || target.isDead() || target.isInvulnerable()) continue;
            if (alreadyHit.contains(target.getUniqueId())) continue;
            if (target instanceof Player p && p.getGameMode() == GameMode.SPECTATOR) continue;
            if (target instanceof Tameable pet && owner.equals(pet.getOwner())) continue;

            BoundingBox box = target.getBoundingBox();
            Vector rel = box.getCenter().subtract(c);

            double l = rel.dot(axis);   // position along the crescent
            double f = rel.dot(dir);    // position forward / backward
            double o = rel.dot(perp);   // thickness direction

            if (Math.abs(l) > radius + box.getWidthX() * 0.5) continue;

            // where the curved blade actually is at this lateral position
            double lc = Math.min(Math.abs(l), radius);
            double arcForward = Math.sqrt(Math.max(0.0, radius * radius - lc * lc)) - radius;

            if (Math.abs(f - arcForward) > 1.6) continue;
            if (Math.abs(o) > 1.2 + box.getHeight() * 0.5) continue;

            alreadyHit.add(target.getUniqueId());

            target.damage(damage, owner);
            if (target.getFireTicks() < fireTicks) {
                target.setFireTicks(fireTicks);
            }
            if (knockback > 0) {
                Vector push = dir.clone().multiply(knockback).setY(0.25);
                target.setVelocity(target.getVelocity().add(push));
            }

            Location hitLoc = box.getCenter().toLocation(world);
            world.spawnParticle(flame, hitLoc, 30, 0.4, 0.5, 0.4, 0.06);
            world.spawnParticle(Particle.LAVA, hitLoc, 6, 0.3, 0.4, 0.3, 0.0);
            world.playSound(hitLoc, Sound.ENTITY_GENERIC_BURN, 0.8f, 1.0f);
        }
    }
}
