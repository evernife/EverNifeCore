package br.com.finalcraft.evernifecore.config.playerdata;

import br.com.finalcraft.evernifecore.EverNifeCore;
import br.com.finalcraft.evernifecore.config.Config;
import br.com.finalcraft.evernifecore.cooldown.Cooldown;
import br.com.finalcraft.evernifecore.cooldown.PlayerCooldown;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.*;

public class PlayerData implements IPlayerData{

    //PlayerData
    protected Config config;
    protected String playerName;
    protected UUID uuid;
    protected long lastSeen;
    protected Map<String, PlayerCooldown> cooldownHashMap = new HashMap<>();

    protected transient Player player = null;
    protected transient boolean recentChanged = false;

    // PDSection Controller
    private final Map<Class<? extends PDSection>, PDSection> mapOfPDSections = new HashMap();

    public Map<Class<? extends PDSection>, PDSection> getMapOfPDSections() {
        return mapOfPDSections;
    }

    @Override
    public <T extends PDSection> T getPDSection(Class<? extends T> pdSectionClass){
        PDSection pdSection = mapOfPDSections.get(pdSectionClass);
        if (pdSection == null){
            pdSection = createPDSection(pdSectionClass);
        }
        return (T)pdSection;
    }

    private  <T extends PDSection> T createPDSection(Class<? extends T> pdSectionClass){
        try {
            Constructor<?> constructor = pdSectionClass.getConstructor(PlayerData.class);
            PDSection pdSection = (PDSection) constructor.newInstance(this);
            mapOfPDSections.put(pdSectionClass, pdSection);
            return (T)pdSection;
        }catch (Exception e){
            EverNifeCore.warning("Failed to instantiate PDSection [" + pdSectionClass.getName() + "]!");
            throw new RuntimeException(e);
        }
    }

    protected PlayerData() {
    }

    public PlayerData(Config config) {
        this.config = Objects.requireNonNull(config,"PlayConfig cannot be null!");
        this.playerName = Objects.requireNonNull(config.getString("PlayerData.Username"),"PlayName cannot be null!");
        this.uuid = Objects.requireNonNull(config.getUUID("PlayerData.UUID"),"PlayerUUID cannot be null!");
        this.lastSeen = config.getLong("PlayerData.lastSeen",0);

        for (String cooldownID : config.getKeys("Cooldowns")) {
            Cooldown cooldown = config.getLoadable("Cooldown." + cooldownID,Cooldown.class);
            PlayerCooldown playerCooldown = new PlayerCooldown(cooldown, this.uuid);
            cooldownHashMap.put(playerCooldown.getIdentifier(), playerCooldown);
        }
    }

    public PlayerData(Config config, String playerName, UUID uuid) {
        this.config = config;
        this.playerName = playerName;
        this.uuid = uuid;
        this.lastSeen = 0;
    }

    public void setRecentChanged(){
        if (this.recentChanged == false)
            this.recentChanged = true;
    }

    //Save Single PlayerData into YML
    public boolean forceSavePlayerData(){
        setRecentChanged();
        return savePlayerData();
    }

    //Save Single PlayerData into YML
    public boolean savePlayerData(){
        if (this.recentChanged){
            //Player Data
            config.setValue("PlayerData.Username",this.playerName);
            config.setValue("PlayerData.UUID",this.uuid);
            config.setValue("PlayerData.lastSeen",this.lastSeen);

            // Loop all PDSections and save than if needed
            for (PDSection pDSection : mapOfPDSections.values()){
                try {
                    if (pDSection.recentChanged){
                        pDSection.savePDSection();
                        pDSection.recentChanged = false;
                    }
                }catch (Throwable e){
                    EverNifeCore.warning("Failed to save PDSection {" + pDSection.getClass().getName() + "} at [" + this.getConfig().getTheFile().getAbsolutePath() + "]");
                    e.printStackTrace();
                }
            }

            try { // Save all Cooldowns
                final List<Cooldown> cooldownList;
                synchronized (cooldownHashMap){
                    cooldownList = new ArrayList<>(cooldownHashMap.values());
                }
                for (Cooldown cooldown : cooldownList) {
                    if (cooldown.isPersistent()){
                        config.setValue("Cooldown." + cooldown.getIdentifier(), cooldown);
                    }
                }
            }catch (Throwable e){
                EverNifeCore.warning("Failed to save Cooldowns for players {" + this.playerName + "} at [" + this.getConfig().getTheFile().getAbsolutePath() + "]");
                e.printStackTrace();
            }

            this.recentChanged = false;
            config.saveAsync();
            return true;
        }
        return false;
    }

    public void setPlayer(Player player){
        this.player = player;
        lastSeen = System.currentTimeMillis();
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public long getLastSeen(){
        return player != null ? System.currentTimeMillis() : lastSeen;
    }

    @Override
    public Player getPlayer(){
        return player;
    }

    @Override
    public boolean isPlayerOnline(){
        return player != null && player.isOnline();
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public PlayerCooldown getCooldown(String identifier){
        return cooldownHashMap.computeIfAbsent(identifier, s -> new PlayerCooldown(identifier, this.getUniqueId()));
    }

    public Map<String, PlayerCooldown> getCooldownHashMap() {
        return cooldownHashMap;
    }

    @Override
    public PlayerData getPlayerData() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;//Only equals when the same object, otherwise different
        return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();//Use UUID as hashcode
    }
}
