package cn.mcd7;

import fr.xephi.authme.api.v3.AuthMeApi;
import me.clip.placeholderapi.PlaceholderAPI;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class AuthMeAnvilLogin extends JavaPlugin implements Listener {
    public static File configFile = new File("./plugins/AuthMeAnvilLogin/config.yml");
    static YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    public static ArrayList<String> unsafePasswords = new ArrayList<>();

    String register = config.getString("title.register", "当前为注册页面的默认值").replace("&", "§");
    String longOrShort = config.getString("title.longOrShort", "当前为密码长度不符合页面的默认值").replace("&", "§");
    String unsafe = config.getString("title.unsafe", "当前为密码不安全页面的默认值").replace("&", "§");
    String login = config.getString("title.login", "当前为登录页面的默认值").replace("&", "§");
    String error = config.getString("title.error", "当前为密码错误页面的默认值").replace("&", "§");
    String clear = config.getString("title.clear", "当前为清空页面的默认值").replace("&", "§");

    String firstChar = config.getString("set.firstChar", "➢ ").replace("&", "").replace("§", "");
    int passwordShort = config.getInt("set.passwordShort", 8);
    int passwordLong = config.getInt("set.passwordLong", 22);

    boolean soundEnable = config.getBoolean("sound.enable");
    String wrong = config.getString("sound.wrong", "none");
    String success = config.getString("sound.success", "none");
    String reset = config.getString("sound.reset", "none");

    String leftItem_type = config.getString("leftItem.type", "MAP").replace("&", "§");
    List<String> leftItem_lore = config.getStringList("leftItem.lore");
    int leftItem_data = config.getInt("leftItem.data");
    String rightItem_name = config.getString("rightItem.name", "第二个物品名称").replace("&", "§");
    String rightItem_type = config.getString("rightItem.type", "MAP").replace("&", "§");
    List<String> rightItem_lore = config.getStringList("rightItem.lore");
    int rightItem_data = config.getInt("rightItem.data");
    String outItemForLogin_name = config.getString("outItemForLogin.name", "第三个物品名称").replace("&", "§");
    String outItemForLogin_type = config.getString("outItemForLogin.type", "MAP").replace("&", "§");
    List<String> outItemForLogin_lore = config.getStringList("outItemForLogin.lore");
    int outItemForLogin_data = config.getInt("outItemForLogin.data");
    String outItemForRegister_name = config.getString("outItemForRegister.name", "第三个物品名称").replace("&", "§");
    String outItemForRegister_type = config.getString("outItemForRegister.type", "MAP").replace("&", "§");
    List<String> outItemForRegister_lore = config.getStringList("outItemForRegister.lore");
    int outItemForRegister_data = config.getInt("outItemForRegister.data");

    AuthMeApi authMeApi = AuthMeApi.getInstance();

    @Override
    public void onLoad() {
        if (AuthMeAnvilLogin.configFile.exists()) {
            return;
        }

        saveResource("config.yml", false);
        getLogger().info("读取默认配置文件成功~");
    }

    @Override
    public void onEnable() {
        unsafePasswords.addAll(config.getStringList("set.unsafePasswords"));
        Bukkit.getLogger().info("铁砧登录 V1.1 启动完成~");
        Bukkit.getLogger().info("作者: wangmeng123(3328429240)");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player eventPlayer = event.getPlayer();
        String eventPlayerName = eventPlayer.getName();

        String register = PlaceholderAPI.setPlaceholders(eventPlayer, this.register);
        String longOrShort = PlaceholderAPI.setPlaceholders(eventPlayer, this.longOrShort);
        String unsafe = PlaceholderAPI.setPlaceholders(eventPlayer, this.unsafe);
        String login = PlaceholderAPI.setPlaceholders(eventPlayer, this.login);
        String error = PlaceholderAPI.setPlaceholders(eventPlayer, this.error);

        ItemStack leftItem = getLeftItem();
        ItemStack rightItem = getRightItem();
        ItemStack outItemForLogin = getOutItemForLogin();
        ItemStack outItemForRegister = getOutItemForRegister();

        AnvilGUI.Builder anvilGUIForLogin = new AnvilGUI.Builder().onClick((slot, snapshot) -> {
            Player player = snapshot.getPlayer();
            String text = snapshot.getText().replace("rr➢", "").replace(firstChar, "");
            String playerName = player.getName();

            List<AnvilGUI.ResponseAction> isLogin = checkPlayerLogin(player, authMeApi);

            if (isLogin != null) {
                return isLogin;
            }

            List<AnvilGUI.ResponseAction> clickMid = clickMidItem(slot, player);

            // 中间物品点击检测
            if (clickMid != null) {
                return clickMid;
            }

            if (authMeApi.checkPassword(playerName, text)) {
                authMeApi.forceLogin(player);
                if (soundEnable) {
                    player.playSound(player.getLocation(), Sound.valueOf(success), 1.0F, 1.0F);
                }
                List<AnvilGUI.ResponseAction> list = new ArrayList<>();
                list.add(AnvilGUI.ResponseAction.close());
                return list;
            } else {
                List<AnvilGUI.ResponseAction> list = new ArrayList<>();
                list.add(AnvilGUI.ResponseAction.replaceInputText(firstChar));
                list.add(AnvilGUI.ResponseAction.updateTitle(error, false));
                return list;
            }
        }).preventClose().itemLeft(leftItem).itemRight(rightItem).itemOutput(outItemForLogin).title(login).plugin(this);

        AnvilGUI.Builder anvilGUIForRegister = new AnvilGUI.Builder().onClick((slot, snapshot) -> {
            Player player = snapshot.getPlayer();
            String text = snapshot.getText().replace("rr➢", "").replace(firstChar, "");

            List<AnvilGUI.ResponseAction> isLogin = checkPlayerLogin(player, authMeApi);

            if (isLogin != null) {
                return isLogin;
            }

            List<AnvilGUI.ResponseAction> clickMid = clickMidItem(slot, player);

            // 中间物品点击检测
            if (clickMid != null) {
                return clickMid;
            }

            if (text.length() < passwordShort || text.length() > passwordLong) {
                if (soundEnable) {
                    player.playSound(player.getLocation(), Sound.valueOf(wrong), 1.0F, 1.0F);
                }
                List<AnvilGUI.ResponseAction> list = new ArrayList<>();
                list.add(AnvilGUI.ResponseAction.replaceInputText(firstChar));
                list.add(AnvilGUI.ResponseAction.updateTitle(longOrShort, false));
                return list;
            }

            if (unsafePasswords.contains(text)) {
                if (soundEnable) {
                    player.playSound(player.getLocation(), Sound.valueOf(wrong), 1.0F, 1.0F);
                }
                List<AnvilGUI.ResponseAction> list = new ArrayList<>();
                list.add(AnvilGUI.ResponseAction.replaceInputText(firstChar));
                list.add(AnvilGUI.ResponseAction.updateTitle(unsafe, false));
                return list;
            }

            authMeApi.forceRegister(player, text, true);
            if (soundEnable) {
                player.playSound(player.getLocation(), Sound.valueOf(success), 1.0F, 1.0F);
            }
            List<AnvilGUI.ResponseAction> list = new ArrayList<>();
            list.add(AnvilGUI.ResponseAction.close());
            return list;
        }).preventClose().itemLeft(leftItem).itemRight(rightItem).itemOutput(outItemForRegister).title(register).plugin(this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (authMeApi.isAuthenticated(eventPlayer) || authMeApi.isUnrestricted(eventPlayer)) {
                return;
            }

            // 如果未登录则打开登录框
            if (!authMeApi.isRegistered(eventPlayerName)) {
                anvilGUIForRegister.open(eventPlayer);
                return;
            }

            anvilGUIForLogin.open(eventPlayer);
        }, 20L);
    }

    public List<AnvilGUI.@NotNull ResponseAction> checkPlayerLogin(Player player, AuthMeApi api) {
        if (api.isAuthenticated(player) || api.isUnrestricted(player)) {
            if (soundEnable) {
                player.playSound(player.getLocation(), Sound.valueOf(success), 1.0F, 1.0F);
            }
            List<AnvilGUI.ResponseAction> list = new ArrayList<>();
            list.add(AnvilGUI.ResponseAction.close());
            return list;
        }
        return null;
    }

    public List<AnvilGUI.ResponseAction> clickMidItem(Integer slot, Player player) {
        String clear = PlaceholderAPI.setPlaceholders(player, this.clear);
        // 中间物品点击处理
        if (slot == AnvilGUI.Slot.INPUT_RIGHT) {
            if (soundEnable) {
                player.playSound(player.getLocation(), Sound.valueOf(reset), 1.0F, 1.0F);
            }
            List<AnvilGUI.ResponseAction> list = new ArrayList<>();
            list.add(AnvilGUI.ResponseAction.replaceInputText(firstChar));
            list.add(AnvilGUI.ResponseAction.updateTitle(clear, false));
            return list;
        }

        return null;
    }

    private @NotNull ItemStack getLeftItem() {
        ItemStack leftItem = new ItemStack(Material.getMaterial(leftItem_type));
        ItemMeta leftMeta = leftItem.getItemMeta();
        leftMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        leftMeta.setDisplayName(firstChar);
        leftMeta.setLore(leftItem_lore);
        if (leftItem_data != 0) {
            leftMeta.setCustomModelData(leftItem_data);
        }
        leftItem.setItemMeta(leftMeta);
        return leftItem;
    }

    private @NotNull ItemStack getRightItem() {
        ItemStack leftItem = new ItemStack(Material.getMaterial(rightItem_type));
        ItemMeta leftMeta = leftItem.getItemMeta();
        leftMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        leftMeta.setDisplayName(rightItem_name);
        leftMeta.setLore(rightItem_lore);
        if (rightItem_data != 0) {
            leftMeta.setCustomModelData(rightItem_data);
        }
        leftItem.setItemMeta(leftMeta);
        return leftItem;
    }

    private @NotNull ItemStack getOutItemForLogin() {
        ItemStack leftItem = new ItemStack(Material.getMaterial(outItemForLogin_type));
        ItemMeta leftMeta = leftItem.getItemMeta();
        leftMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        leftMeta.setDisplayName(outItemForLogin_name);
        leftMeta.setLore(outItemForLogin_lore);
        if (outItemForLogin_data != 0) {
            leftMeta.setCustomModelData(outItemForLogin_data);
        }
        leftItem.setItemMeta(leftMeta);
        return leftItem;
    }

    private @NotNull ItemStack getOutItemForRegister() {
        ItemStack leftItem = new ItemStack(Material.getMaterial(outItemForRegister_type));
        ItemMeta leftMeta = leftItem.getItemMeta();
        leftMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        leftMeta.setDisplayName(outItemForRegister_name);
        leftMeta.setLore(outItemForRegister_lore);
        if (outItemForRegister_data != 0) {
            leftMeta.setCustomModelData(outItemForRegister_data);
        }
        leftItem.setItemMeta(leftMeta);
        return leftItem;
    }
}
