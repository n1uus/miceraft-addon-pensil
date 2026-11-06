// pensil (Bedrock): рейды, месть за убийство, случайные события, страх.
import { world, system, ItemStack } from "@minecraft/server";

const ID = "hammer:random_hammer";
const CFG = {
  tntCount: 10,          // сколько тнт
  tntArea: 50,           // сторона квадрата, где появляется тнт
  lightning: 2,          // сколько молний бьёт в убийцу
  raidWaves: 5,          // волн в рейде
  raidCooldownTicks: 12000,
  eventChance: 0.1,      // шанс события каждые 30 сек на игрока
  fearAura: true,
};

const rnd = (a, b) => a + Math.random() * (b - a);
const rint = (n) => Math.floor(Math.random() * n);
const say = (p, t) => { try { p.sendMessage(t); } catch (e) {} };
const safe = (fn) => { try { return fn(); } catch (e) { return undefined; } };

function surfaceY(dim, x, z, fallback) {
  const b = safe(() => dim.getTopmostBlock({ x: Math.floor(x), z: Math.floor(z) }));
  return b ? b.location.y + 1 : fallback;
}

function spawnPensil(dim, x, y, z, raid) {
  const e = safe(() => dim.spawnEntity(ID, { x, y, z }));
  if (e && raid) safe(() => e.addTag("pensil_raid"));
  return e;
}

// ---------------- месть: молнии, потом тнт ----------------
const storms = new Set();

function startStorm(player) {
  if (storms.has(player.id)) return;
  storms.add(player.id);
  const step = 15;
  for (let i = 0; i < CFG.lightning; i++) {
    system.runTimeout(() => {
      safe(() => player.dimension.spawnEntity("minecraft:lightning_bolt", player.location));
    }, i * step);
  }
  system.runTimeout(() => {
    safe(() => {
      for (let i = 0; i < CFG.tntCount; i++) {
        const h = CFG.tntArea / 2;
        const loc = { x: player.location.x + rnd(-h, h), y: player.location.y + rnd(5, 10), z: player.location.z + rnd(-h, h) };
        const tnt = player.dimension.spawnEntity("minecraft:tnt", loc);
        homing(tnt, player);
      }
    });
    system.runTimeout(() => storms.delete(player.id), 120);
  }, CFG.lightning * step + 10);
}

// тнт летит на игрока (телепортом, 80 тиков — пока горит фитиль)
function homing(tnt, player) {
  let t = 0;
  const id = system.runInterval(() => {
    t++;
    try {
      const p = player.location;
      const c = tnt.location;
      const dx = p.x - c.x, dy = p.y + 1 - c.y, dz = p.z - c.z;
      const d = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1;
      const s = Math.min(0.7, d);
      tnt.teleport({ x: c.x + (dx / d) * s, y: c.y + (dy / d) * s, z: c.z + (dz / d) * s });
    } catch (e) {
      system.clearRun(id);
      return;
    }
    if (t > 100) system.clearRun(id);
  }, 1);
}

world.afterEvents.entityDie.subscribe((ev) => {
  const dead = ev.deadEntity;
  if (!dead || dead.typeId !== ID) return;
  const killer = ev.damageSource && ev.damageSource.damagingEntity;
  if (killer && killer.typeId === "minecraft:player" && !safe(() => dead.hasTag("pensil_raid"))) startStorm(killer);
});

// ---------------- рейд на деревню ----------------
const raids = new Map();        // player.id -> raid
const nextRaid = new Map();     // player.id -> tick

function isAlive(id) { return !!safe(() => world.getEntity(id)); }

function bar(player, text) { safe(() => player.onScreenDisplay.setActionBar(text)); }

function spawnWave(player, raid) {
  raid.wave++;
  raid.total = 2 + raid.wave * 2;
  raid.mobs = [];
  for (let i = 0; i < raid.total; i++) {
    const a = Math.random() * Math.PI * 2, d = rnd(22, 32);
    const x = player.location.x + Math.cos(a) * d, z = player.location.z + Math.sin(a) * d;
    const e = spawnPensil(player.dimension, x, surfaceY(player.dimension, x, z, player.location.y), z, true);
    if (e) raid.mobs.push(e.id);
  }
  say(player, `§c§lВолна ${raid.wave}/${CFG.raidWaves}`);
  safe(() => player.onScreenDisplay.setTitle(`§cВолна ${raid.wave}/${CFG.raidWaves}`));
  safe(() => player.playSound("mob.wither.spawn"));
}

system.runInterval(() => {
  const now = system.currentTick;
  for (const player of world.getAllPlayers()) {
    if (raids.has(player.id)) continue;
    if (player.dimension.id !== "minecraft:overworld") continue;
    if (now < (nextRaid.get(player.id) || 0)) continue;
    const v = safe(() => player.dimension.getEntities({ type: "minecraft:villager", location: player.location, maxDistance: 40 })) || [];
    if (v.length >= 3) {
      raids.set(player.id, { player, wave: 0, total: 1, mobs: [], delay: 100 });
      say(player, "§4§lНа деревню надвигается рейд pensil!");
      safe(() => player.playSound("mob.wither.spawn"));
    }
  }
}, 100);

system.runInterval(() => {
  for (const [pid, raid] of [...raids]) {
    const player = raid.player;
    const valid = safe(() => player.isValid !== false && player.dimension.id === "minecraft:overworld" && player.getComponent("health").currentValue > 0);
    if (!valid) {
      raids.delete(pid);
      nextRaid.set(pid, system.currentTick + CFG.raidCooldownTicks);
      continue;
    }
    if (raid.delay > 0) {
      raid.delay -= 20;
      bar(player, "§dРейд pensil — приготовьтесь...");
      if (raid.delay <= 0) spawnWave(player, raid);
      continue;
    }
    raid.mobs = raid.mobs.filter(isAlive);
    bar(player, `§dРейд pensil — Волна ${raid.wave}/${CFG.raidWaves}  (осталось ${raid.mobs.length})`);
    if (raid.mobs.length > 0) continue;
    if (raid.wave >= CFG.raidWaves) {
      say(player, "§a§lРейд pensil отражён! Вы герой деревни.");
      safe(() => player.addEffect("village_hero", 24000, { amplifier: 0 }));
      safe(() => player.dimension.spawnItem(new ItemStack("minecraft:emerald", 8), player.location));
      safe(() => player.dimension.spawnItem(new ItemStack("minecraft:diamond", 2), player.location));
      raids.delete(pid);
      nextRaid.set(pid, system.currentTick + CFG.raidCooldownTicks);
    } else {
      say(player, `§eВолна ${raid.wave}/${CFG.raidWaves} пройдена. Следующая скоро...`);
      raid.delay = 120;
    }
  }
}, 20);

// ---------------- случайные события ----------------
function ev_skyRain(p) {
  say(p, "§4С неба падают pensil!");
  for (let i = 0; i < 6; i++) spawnPensil(p.dimension, p.location.x + rnd(-12, 12), p.location.y + rnd(25, 35), p.location.z + rnd(-12, 12), false);
}
function ev_whispers(p) {
  say(p, "§8§oТы слышишь, как pensil шепчет твоё имя...");
  safe(() => p.addEffect("darkness", 240, { amplifier: 0, showParticles: false }));
  safe(() => p.playSound("mob.warden.heartbeat"));
  safe(() => p.playSound("ambient.weather.thunder"));
}
function ev_gift(p) {
  say(p, "§dpensil оставил тебе подарок...");
  const items = ["minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot", "minecraft:iron_ingot"];
  const name = items[rint(items.length)];
  safe(() => p.dimension.spawnItem(new ItemStack(name, 1 + rint(3)), { x: p.location.x + 2, y: p.location.y + 1, z: p.location.z }));
}
function ev_jumpscare(p) {
  say(p, "§4Оглянись...");
  safe(() => p.playSound("mob.warden.nearby_closest"));
  const v = p.getViewDirection();
  const x = p.location.x - v.x * 6, z = p.location.z - v.z * 6;
  spawnPensil(p.dimension, x, surfaceY(p.dimension, x, z, p.location.y), z, false);
}
const EVENTS = [ev_skyRain, ev_whispers, ev_gift, ev_jumpscare];

system.runInterval(() => {
  for (const p of world.getAllPlayers()) {
    if (raids.has(p.id) || Math.random() > CFG.eventChance) continue;
    safe(() => EVENTS[rint(EVENTS.length)](p));
  }
}, 600);

// ---------------- страх: рядом с pensil темнеет ----------------
if (CFG.fearAura) {
  system.runInterval(() => {
    for (const p of world.getAllPlayers()) {
      const near = safe(() => p.dimension.getEntities({ type: ID, location: p.location, maxDistance: 14 })) || [];
      if (near.length > 0) {
        safe(() => p.addEffect("darkness", 100, { amplifier: 0, showParticles: false }));
        if (Math.random() < 0.25) safe(() => p.playSound("mob.warden.ambient"));
      }
    }
  }, 40);
}
