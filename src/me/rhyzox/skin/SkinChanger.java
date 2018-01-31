package me.rhyzox.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_11_R1.Packet;
import net.minecraft.server.v1_11_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_11_R1.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_11_R1.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;


public class SkinChanger extends JavaPlugin implements CommandExecutor {

    private HashMap<CraftPlayer, Double> health;
    private HashMap<CraftPlayer, Location> loc;

    @Override
    public void onEnable() {
        health = new HashMap<>();
        loc = new HashMap<>();
        getCommand("skin").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if(sender instanceof Player){
                changeSkin((CraftPlayer)sender, "ungespielt");
        }
        return false;
    }

    public void changeSkin(CraftPlayer cp, String nameFromPlayer){
        GameProfile skingp = cp.getProfile();
        try {
            skingp = GameProfileBuilder.fetch(UUIDFetcher.getUUIDOf(nameFromPlayer));
        } catch (Exception e) {
            cp.sendMessage("§cDer Skin konnte nicht geladen werden");
            e.printStackTrace();
            return;
        }
        Collection<Property> props = skingp.getProperties().get("textures");
        cp.getProfile().getProperties().removeAll("textures");
        cp.getProfile().getProperties().putAll("textures", props);
        loc.put(cp, cp.getLocation().add(0, 0.5, 0));
        health.put(cp, cp.getHealth());

        //region Packets
        PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(cp.getEntityId());
        sendPacket(destroy);

        PacketPlayOutPlayerInfo destroyinfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, cp.getHandle());
        sendPacket(destroyinfo);

        cp.setHealth(0);
        new BukkitRunnable(){
            @Override
            public void run(){
                cp.spigot().respawn();
                cp.setHealth(health.get(cp));
                cp.teleport(loc.get(cp));
                PacketPlayOutPlayerInfo addinfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, cp.getHandle());
                sendPacket(addinfo);

                PacketPlayOutNamedEntitySpawn spawncp = new PacketPlayOutNamedEntitySpawn(cp.getHandle());
                for(Player all : Bukkit.getOnlinePlayers()){
                    if(!all.getName().equals(cp.getName())){
                        ((CraftPlayer)all).getHandle().playerConnection.sendPacket(spawncp);
                    }
                }
            }
        }.runTaskLater(this, 4);

        System.out.println("LOL5");
        //endregion
    }

    public void sendPacket(Packet packet){
        for(Player all : Bukkit.getOnlinePlayers()){
            ((CraftPlayer)all).getHandle().playerConnection.sendPacket(packet);
        }
    }
}