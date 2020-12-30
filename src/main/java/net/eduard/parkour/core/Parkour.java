package net.eduard.parkour.core;

import java.util.HashMap;

import net.eduard.api.lib.modules.Extra;
import net.eduard.api.lib.modules.Mine;
import net.eduard.parkour.EduParkour;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.eduard.api.lib.config.Config;
import net.eduard.api.server.minigame.Minigame;
import net.eduard.api.server.minigame.MinigameMap;
import net.eduard.api.server.minigame.MinigameRoom;

public class Parkour extends Minigame {


    private final EduParkour plugin;

    public Parkour() {
        super("Parkour","");
        this.plugin = EduParkour.getInstance();
    }

    public ItemStack setSpawnItem = Mine.newItem(Material.STICK,
            "§e§lEscolha o Spawn");
    public ItemStack setEndItem = Mine.newItem(Material.BLAZE_ROD,
            "§d§lEscolha o Fim");
    public ItemStack confirmItem = Mine.newItem(Material.BEACON,
            "§a§lConfime o Parkour");
    public ItemStack cancelItem = Mine.newItem(Material.BED,
            "§c§lDelete o Parkour");
    private final HashMap<Player, Location> checkpoints = new HashMap<>();
    private final HashMap<Player, Integer> falls = new HashMap<>();
    private final HashMap<Player, MinigameMap> criando = new HashMap<>();
    private Material checker = Material.DIAMOND_BLOCK;
    private double reward = 100;

    public boolean join(Player player, String name) {
        if (hasMap(name) & !isPlaying(player)) {
            joinPlayer(getGame(name), player);
            Mine.saveItems(player);
            Mine.refreshAll(player);
            chat("Join", player);
            return true;
        }
        return false;
    }


    public void chat(String key, Player player) {
        player.sendMessage(
                Mine.getReplacers(plugin.message(key), player));

    }

    public void leave(Player player) {
        if (hasLobby())
            player.teleport(getLobby());
        chat("Quit", player);
        remove(player);
    }

    public Material getChecker() {
        return checker;
    }

    public void setChecker(Material checker) {
        this.checker = checker;
    }

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public void win(Player player) {
        MinigameRoom arena = getGame(player);
        chat("Win", player);
        for (Player p : arena.getPlayersOnline()) {
            chat("WinBroadcast", p);
        }
        if (hasLobby())
            player.teleport(getLobby());
        remove(player);
    }

    public int getFalls(Player player) {
        return falls.getOrDefault(player, 0);
    }

    public void updateFall(Player player) {
        falls.put(player, getFalls(player));
    }

    public void toCheckpoint(Player p) {
        if (checkpoints.containsKey(p)) {
            p.teleport(checkpoints.get(p));
        } else {
            MinigameRoom game = getGame(p);
            p.teleport(game.getMap().getSpawn());
        }

    }

    public void updateCheckpoint(Player p) {
        if (checkpoints.containsKey(p)) {
            Location check = checkpoints.get(p);
            checkpoints.put(p, p.getLocation());
            if (Mine.equals(check, p.getLocation())) {
                return;
            }
        }
        checkpoints.put(p, p.getLocation());
        chat("CheckPoint", p);

    }

    public Config getConfig(String name) {
        return plugin.getConfigs().createConfig("Arenas/" + name + ".yml");
    }

    public static void play(Player p) {

    }

    public void createNewMap(Player p, String name) {
        PlayerInventory inv = p.getInventory();
        inv.setItem(0, confirmItem);
        inv.setItem(2, setSpawnItem);
        inv.setItem(6, setEndItem);
        inv.setItem(8, cancelItem);
        MinigameMap map = new MinigameMap(this, name, name);
        p.setGameMode(GameMode.CREATIVE);
        map.setSpawn(p.getLocation());
        map.getLocations().put("end", p.getLocation().add(0,2,0));
        criando.put(p, map);
        chat("Creating", p);

    }
    // criando mapa

    @EventHandler
    public void criandoMapa(PlayerInteractEvent e) {

        Player p = e.getPlayer();

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getItem() == null)
                return;
            if (criando.containsKey(p)) {
                MinigameMap map = criando.get(p);
                if (setSpawnItem.equals(e.getItem())) {
                    chat("SetSpawn", p);
                    map.setSpawn(e.getClickedBlock().getLocation());
                } else if (setEndItem.equals(e.getItem())) {

                    chat("SetEnd", p);
                    map.getLocations().put("end",
                            e.getClickedBlock().getLocation());

                }
            }
        }
    }

    @EventHandler
    public void deletandoOuConfirmandoParkour(PlayerInteractEvent e) {

        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK ||
                e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (e.getItem() == null)
                return;
            if (criando.containsKey(p)) {
                MinigameMap map = criando.get(p);
                if (confirmItem.equals(e.getItem())) {

                    chat("Create", p);

                    new MinigameRoom(this, map);
                    criando.remove(p);
                    Mine.refreshAll(p);
                } else if (cancelItem.equals(e.getItem())) {

                    chat("Delete", p);

                    criando.remove(p);
                    removeMap(map);
                    Mine.refreshAll(p);
                }
            }
        }
    }

    public void remove(Player player) {
        Mine.getItems(player);
        falls.remove(player);
        checkpoints.remove(player);
        super.remove(player);

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoRenascer(PlayerRespawnEvent e) {

        Player p = e.getPlayer();
        if (isPlaying(p)) {
            e.setRespawnLocation(getLobby());
            updateFall(p);

        }
    }

    @EventHandler
    public void aoLevarDano(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (e.getCause() == DamageCause.FALL) {
                if (isPlaying(p)) {
                    updateFall(p);
                    toCheckpoint(p);
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void aoTirarPvP(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player
                & e.getDamager() instanceof Player) {
            Player p = (Player) e.getEntity();
            Player d = (Player) e.getDamager();
            if (isPlaying(p)) {
                e.setCancelled(true);
            }
            if (isPlaying(d)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void aoDigitarComando(PlayerCommandPreprocessEvent e) {

        Player p = e.getPlayer();
        if (isPlaying(p)) {
            if (Extra.startWith(e.getMessage(), "/leave")
                    || Extra.startWith(e.getMessage(), "/sair")) {
                leave(p);
            } else {
                chat("OnlyQuit", p);
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void aoMover(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (Mine.equals(e.getFrom(), e.getTo())) return;
        if (isPlaying(p)) {
            MinigameMap map = getGame(p).getMap();
            if (e.getTo().getBlock().getRelative(BlockFace.DOWN)
                    .getType() == getChecker()) {
                updateCheckpoint(p);
            }
            if ( map.getLocations().get("end") == null)return;
            if (Mine.equals(e.getTo(), map.getLocations().get("end"))) {
                win(p);
            }

        } else {
            for (MinigameMap map : getMaps()) {
                if (map.getSpawn() == null)continue;
                if (Mine.equals(e.getTo(), map.getSpawn())) {
                    join(p , map.getName());
                    break;
                }
            }

        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        remove(e.getPlayer());
    }

    @EventHandler
    public void aoSerKitado(PlayerKickEvent e) {
        remove(e.getPlayer());
    }

    @EventHandler
    public void semFome(FoodLevelChangeEvent e) {

        HumanEntity who = e.getEntity();
        if (who instanceof Player) {
            Player p = (Player) who;
            if (isPlaying(p)) {
                e.setFoodLevel(20);
                p.setSaturation(20);
                p.setExhaustion(0);
            }

        }
    }


}
