package br.com.finalcraft.evernifecore.config;

import br.com.finalcraft.evernifecore.EverNifeCore;
import br.com.finalcraft.evernifecore.config.yaml.anntation.Salvable;
import br.com.finalcraft.evernifecore.config.yaml.exeption.LoadableMethodException;
import br.com.finalcraft.evernifecore.config.yaml.helper.CfgExecutor;
import br.com.finalcraft.evernifecore.config.yaml.helper.CfgLoadableSalvable;
import br.com.finalcraft.evernifecore.config.yaml.helper.ConfigHelper;
import br.com.finalcraft.evernifecore.config.yaml.helper.smartloadable.SmartLoadSave;
import br.com.finalcraft.evernifecore.config.yaml.section.ConfigSection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.comments.CommentType;
import org.simpleyaml.configuration.comments.format.YamlCommentFormat;
import org.simpleyaml.configuration.file.YamlConfigurationOptions;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.exceptions.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Config {

    private static final Logger logger = Logger.getLogger("FCConfig");

    protected final YamlFile yamlFile;
    protected final ReentrantLock lock = new ReentrantLock(true);

    protected transient long lastModified;
    protected transient boolean newDefaultValueToSave = false;

    // ------------------------------------------------------------------------------------------------------------------
    //      Load Function
    // ------------------------------------------------------------------------------------------------------------------

    private void loadWithComments(){
        if (getTheFile().exists()){
            try {
                this.yamlFile.loadWithComments();
            } catch (IOException e) {
                //In case of an error, usually by a MalFormed YML File, it's better to create a new file and just notify the console
                EverNifeCore.warning(String.format("Failed to load YML file at [%s]", this.getAbsolutePath()));
                e.printStackTrace();
                try {
                    FileUtils.copyFile(this.getTheFile(), new File(this.getTheFile().getParentFile(), this.getTheFile().getName() + ".corrupted"));
                }catch (Exception e2){
                    EverNifeCore.instance.getLogger().log(Level.SEVERE, String.format("[SEVERE_ERROR] Failed to create a COPY of the corrupted file at [%s]!", this.getAbsolutePath()));
                    e2.printStackTrace();
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    //      Constructors
    // ------------------------------------------------------------------------------------------------------------------

    public Config(YamlFile yamlFile) {
        this.yamlFile = yamlFile;
        this.lastModified = getTheFile() == null ? 0 : getTheFile().lastModified();

        loadWithComments(); //Do file Loading if exists
    }

    public Config(File theFile) {
        this(new YamlFile(theFile));
    }

    public Config(String path) {
        this(new File(path));
    }

    // ------------------------------------------------------------------------------------------------------------------
    //      ECPlugins Constructors
    // ------------------------------------------------------------------------------------------------------------------

    public Config(Plugin plugin, String configName, boolean copyDefaults) {
        File targetFile = new File(plugin.getDataFolder(), configName);

        if (!targetFile.exists() && copyDefaults) {
            try {
                ConfigHelper.copyAsset(plugin, configName, plugin.getDataFolder());
            }catch (IOException e){
                plugin.getLogger().warning("Failed to load Asset for the config [" + configName + "]!");
                e.printStackTrace();
            }
        }

        this.yamlFile = new YamlFile(targetFile);
        this.lastModified = targetFile.lastModified();

        yamlFile.setCommentFormat(YamlCommentFormat.PRETTY);

        this.loadWithComments(); //Do file Loading if exists

        this.yamlFile.options().headerFormatter()
                .prefixFirst("# -----------------------------------------------------")
                .suffixLast("\n# ----------------------------------------------------");

        this.yamlFile.options().header(
                // Site that i used to make this http://patorjk.com/software/taag/#p=display&f=Doom&t=FinalCraft
                "" +
                        "\n        _____ _____              __ _       " +
                        "\n       |  ___/  __ \\            / _(_)      " +
                        "\n       | |__ | /  \\/ ___  _ __ | |_ _  __ _ " +
                        "\n       |  __|| |    / _ \\| '_ \\|  _| |/ _` |" +
                        "\n       | |___| \\__/\\ (_) | | | | | | | (_| |" +
                        "\n       \\____/ \\____/\\___/|_| |_|_| |_|\\__, |" +
                        "\n                                       __/ |" +
                        "\n                                      |___/ " +
                        "\n  " +
                        "\n  " +
                        "\n  " +
                        "\n              EverNife's Config Manager" +
                        "\n" +
                        "\n Plugin: " + plugin.getName() +
                        "\n Author: " + (plugin.getDescription().getAuthors().size() > 0 ? plugin.getDescription().getAuthors().get(0) : "Desconhecido") +
                        "\n"
        );
    }

    public Config(Plugin plugin, String configName) {
        this(plugin,configName, false);
    }

    // ------------------------------------------------------------------------------------------------------------------
    //      File Related (save/load/reload)
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the File the Config is handling
     *
     * It will probably always be non-null, but it can still be null :D
     *
     * @return  The File this Config is handling
     */
    public @Nullable File getTheFile() {
        return this.yamlFile.getConfigurationFile();
    }

    /**
     * If the file exists, return the file path, otherwise return null
     *
     * @return The absolute path of the file.
     */
    public @Nullable String getAbsolutePath(){
        return this.yamlFile.getConfigurationFile() != null ? this.yamlFile.getConfigurationFile().getAbsolutePath() : null;
    }

    /**
     * Gets the YamlFile of this Config
     *
     * @return  The FileConfiguration Object
     */
    public YamlFile getConfiguration() {
        return this.yamlFile;
    }

    /**
     * Saves the Config Object to it's File
     *
     * This is a synchronized action using a {@link ReentrantLock}
     */
    public void save() {
        lock.lock();
        try {
            yamlFile.save();
        } catch (IOException e) {
            logger.warning("Failed to save file [" + getTheFile().getAbsolutePath() + "]");
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    /**
     * Saves the Config Object to a File
     *
     * @param  file The File you are saving this Config to
     */
    public void save(File file) {
        try {
            yamlFile.save(file);
        } catch (IOException e) {
            logger.warning("Failed to save file [" + file.getAbsolutePath() + "]");
            e.printStackTrace();
        }
    }

    /**
     * If the user has changed the default value, save it
     */
    public void saveIfNewDefaults() {
        if (newDefaultValueToSave){
            save();
            newDefaultValueToSave = false;
        }
    }

    /**
     * Saves the Config Object to its File,
     * and ensure it's called async
     */
    public void saveAsync() {
        CfgExecutor.getExecutorService().submit(() -> this.save());
    }

    /**
     * Reloads the Configuration File
     */
    public void reload() {
        try {
            if (getTheFile().exists()){ //Reoad from the file
                loadWithComments();
                this.lastModified = getTheFile().lastModified();
            }else { //Otherwise, read from an EmptyString
                this.yamlFile.loadFromString("");
                this.lastModified = 0;
            }
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the time when this config was last modified.
     *
     * @return The last modified time of the config.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * If the file doesn't exist, or if the last modified time of the file is different than the last modified time of the
     * file when it was last read, then the file has been modified
     *
     * @return A boolean value.
     */
    public boolean hasBeenModified(){
        if (getTheFile() == null || !getTheFile().exists()){
            return lastModified != 0;
        }

        return lastModified != getTheFile().lastModified();
    }

    // ------------------------------------------------------------------------------------------------------------------
    //      Comment System Functions
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Set a comment to the section or value selected by path.
     * Comment will be indented automatically.
     * Multi-line comments can be provided using \n character.
     * <p/>
     * Comment format will follow the rules of {@link YamlFile#options()} {@link YamlConfigurationOptions#commentFormatter()}.
     *
     * @param path    path of desired section or key
     * @param comment the comment to add, # prefix is not needed
     * @param type    either above (BLOCK) or SIDE
     */
    public void setComment(@NotNull String path, @NotNull String comment, @NotNull CommentType type) {
        this.yamlFile.setComment(path, comment, type);
    }

    /**
     * Set a block comment above the section or value selected by path.
     * Comment will be indented automatically.
     * Multi-line comments can be provided using \n character.
     * <p/>
     * Comment format will follow the rules of {@link YamlFile#options()} {@link YamlConfigurationOptions#commentFormatter()}.
     *
     * @param path    path of desired section or key
     * @param comment the block comment to add, # character is not needed
     */
    public void setComment(@NotNull String path, @Nullable String comment) {
        this.yamlFile.setComment(path, comment);
    }

    /**
     * Retrieve the comment of the section or value selected by path.
     * <p/>
     * Comment format will follow the rules of {@link YamlFile#options()} {@link YamlConfigurationOptions#commentFormatter()}.
     *
     * @param path path of desired section or key
     * @param type either above (BLOCK) or SIDE
     * @return the comment of the section or value selected by path,
     * or null if that path does not have any comment of this type
     */
    public String getComment(@NotNull String path, @NotNull CommentType type) {
        return this.yamlFile.getComment(path, type);
    }

    /**
     * Retrieve the block comment of the section or value selected by path.
     * <p/>
     * Comment format will follow the rules of {@link YamlFile#options()} {@link YamlConfigurationOptions#commentFormatter()}.
     *
     * @param path path of desired section or key
     * @return the block comment of the section or value selected by path,
     * or null if that path does not have any comment of type block
     */
    public String getComment(@NotNull String path) {
        return this.yamlFile.getComment(path);
    }

    // ------------------------------------------------------------------------------------------------------------------
    //      Basic YamlFile Functions
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Actually store the object on the YamlFile
     *
     * @param  path The path in the Config File
     * @param  value The Value for that Path
     */
    protected void store(String path, Object value) {
        this.yamlFile.set(path, value);
    }

    public ConfigSection getConfigSection(String path){
        return new ConfigSection(this, path);
    }

    public ConfigurationSection getConfigurationSection(String path){
        return getConfiguration().getConfigurationSection(path);
    }

    /**
     * Returns all Paths in this Config
     *
     * @return All Paths in this Config
     */
    public Set<String> getKeys() {
        return yamlFile.getKeys(false);
    }

    /**
     * Returns all Sub-Paths in this Config
     *
     * @param  path The path in the Config File
     * @return All Sub-Paths of the specified Path
     */
    public Set<String> getKeys(String path) {
        if (contains(path)){
            ConfigurationSection configurationSection = yamlFile.getConfigurationSection(path);
            if (configurationSection != null){ //Even containing this path, this path might not be a ConfigurationSection
                return configurationSection.getKeys(false);
            }
        }
        return Collections.emptySet();
    }

    /**
     * Returns all Sub-Paths in this Config
     *
     * @param  path The path in the Config File
     * @param  deep If to do a deep serach
     * @return All Sub-Paths of the specified Path
     */
    public Set<String> getKeys(String path, boolean deep) {
        if (contains(path)){
            return yamlFile.getConfigurationSection(path).getKeys(deep);
        }
        return Collections.emptySet();
    }

    /**
     * Checks whether the Config contains the specified Path
     *
     * @param  path The path in the Config File
     * @return      True/false
     */
    public boolean contains(String path) {
        return yamlFile.contains(path);
    }

    /**
     * Returns the Object at the specified Path
     *
     * @param  path The path in the Config File
     * @return      The Value at that Path
     */
    public Object getValue(String path) {
        return yamlFile.get(path);
    }

    /**
     * Sets the Value for the specified Path
     * Also, if the value is not null, set the comment.
     *
     * @param path The path in the Config File
     * @param value The Value for that Path
     * @param comment The comment to add to the path
     */
    public void setValue(String path, Object value, String comment) {
        this.setValue(path, value);
        if (value != null){ //If this section has been erased, ignore the comment
            setComment(path, comment);
        }
    }

    /**
     * Sets the Value for the specified Path
     *
     * @param  path The path in the Config File
     * @param  value The Value for that Path
     */
    public void setValue(String path, Object value) {
        //Remove Behavior
        if (value == null) {
            this.store(path, null);
            return;
        }

        //Checking for Interables of Salvables
        if (value instanceof Iterable){ //Lits, Sets, and all the rest
            Iterator iterator = ((Iterable) value).iterator();
            if (iterator.hasNext()){ //Has at least one element
                Object firstValue = iterator.next();

                //IF this LIST is a Customizable Salvable
                SmartLoadSave smartLoadSave = CfgLoadableSalvable.getLoadableStatus(firstValue.getClass());
                if (smartLoadSave != null && smartLoadSave.isSalvable()){
                    //Lets create a temporary list holding all elements!
                    List newList = new ArrayList();
                    newList.add(firstValue);
                    iterator.forEachRemaining(o -> newList.add(o));

                    //Erase previous list saved there
                    this.setValue(path,null);

                    if (smartLoadSave.canSerializeToStringList()){
                        //We can serialize this object into a StringList
                        setValue(path,
                                newList.stream().map(o -> smartLoadSave.onStringSerialize(o)).collect(Collectors.toList())
                        );
                        return;
                    }

                    //If does not support being saved on a Single String, we need to create custom Indexes
                    //Save each element under "path.index"

                    //There are two cases
                        //The object does not use it's KEY as argument, so the index can be a simple number
                        //The object uses the KEY as argument to be loaded, the index is a custom string

                    //To find out, lets save the first value.
                    //Yes, at each Iterable<> we waste one operation when the LoadableSalvable use a custom index
                    this.setValue(path + ".0", newList.get(0));

                    boolean useCustomIndex = false;

                    //If the first value saved creates a SINGLE section inside itself to save it's contents, it means
                    //it uses the KEY as an attribute

                    //TODO Replace this system on the future, creating a variable inside the ConfigSection and
                    // make the setValue() return this ConfigSection, even as a private Function

                    Set<String> keys = this.getKeys(path + ".0");
                    if (keys.size() == 1){
                        String theOnlyKey = keys.iterator().next();
                        useCustomIndex = this.yamlFile.isConfigurationSection(path + ".0." + theOnlyKey);
                        if (useCustomIndex){
                            this.setValue(path + ".0", null); //Erase the written value
                        }
                    }

                    for (int count = useCustomIndex ? 0 : 1; count < newList.size(); count++) {
                        String index = useCustomIndex == false
                                ? "." + count //Simple Numbers as Index
                                : ""; //Leave empty, it means the LoadableSalvable will create its own index

                        this.setValue(path + index, newList.get(count));
                    }
                    return;
                }
            }
        }

        //IF it is a Customizable Salvable
        SmartLoadSave smartLoadSave = CfgLoadableSalvable.getLoadableStatus(value.getClass());
        if (smartLoadSave != null && smartLoadSave.isSalvable()){
            smartLoadSave.onConfigSave(new ConfigSection(this, path), value);
            return;
        }

        //For Simple Salvables
        if (value instanceof Salvable){
            ((Salvable) value).onConfigSave(new ConfigSection(this, path));
            return;
        }

        //UUID should be saved as String
        if (value instanceof UUID) {
            this.store(path, value.toString());
            return;
        }

        //Default YML behavior
        this.store(path, value);
    }

    /**
     * Sets the Value for the specified Path
     * (IF the Path does not yet exist)
     *
     * @param  path The path in the Config File
     * @param  value The Value for that Path
     */
    public void setDefaultValue(String path, Object value) {
        getOrSetDefaultValue(path, value);
    }

    /**
     * Sets the Value for the specified Path
     * (IF the Path does not yet exist)
     * Sets the Comment for the specified Path
     * (IF the Path's comment is different from the passed one)
     *
     * @param  path The path in the Config File
     * @param  value The Value for that Path
     * @param  comment The Comment for that Path
     */
    public void setDefaultValue(String path, Object value, String comment) {
        getOrSetDefaultValue(path, value, comment);
    }

    // ------------------------------------------------------------------------------------------------------------------
    //      Basic Java Elements Getters
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the String at the specified Path
     *
     * @param  path The path in the Config File
     * @return The String at that Path
     */
    public String getString(String path) {
        return yamlFile.getString(path);
    }

    public String getString(String path, String def) {
        return yamlFile.getString(path,def);
    }

    /**
     * Returns the Boolean at the specified Path
     *
     * @param  path The path in the Config File
     * @return The Boolean at that Path
     */
    public boolean getBoolean(String path) {
        return yamlFile.getBoolean(path);
    }
    public boolean getBoolean(String path, boolean def) {
        return yamlFile.getBoolean(path,def);
    }

    /**
     * Returns the Integer at the specified Path
     *
     * @param  path The path in the Config File
     * @return The Integer at that Path
     */
    public int getInt(String path) {
        return yamlFile.getInt(path);
    }
    public int getInt(String path, int def) {
        return yamlFile.getInt(path,def);
    }

    /**
     * Returns the Long at the specified Path
     *
     * @param  path The path in the Config File
     * @return The Long at that Path
     */
    public long getLong(String path) {
       return getLong(path, 0);
    }
    public long getLong(String path, long def) {
        if (!contains(path)){
            return def;
        }
        //Fix for old behavior of this config, we previously stringify 'long' values, why? who knows :/
        if (yamlFile.isString(path)){
            return Long.valueOf(yamlFile.getString(path));
        }else {
            return yamlFile.getLong(path);
        }
    }

    /**
     * Returns the Double at the specified Path
     *
     * @param  path The path in the Config File
     * @return      The Double at that Path
     */
    public double getDouble(String path) {
        return yamlFile.getDouble(path);
    }
    public double getDouble(String path, double def) {
        return yamlFile.getDouble(path,def);
    }

    /**
     * Returns the UUID at the specified Path
     *
     * @param  path The path in the Config File
     * @return The UUID at that Path
     */
    public UUID getUUID(String path) {
        String value = getString(path);
        return value == null ? null : UUID.fromString(value);
    }

    /**
     * If the UUID at the given path is not null, return it, otherwise return the default UUID
     *
     * @param path The path to the value you want to get.
     * @param def The default value to return if the path is not found.
     * @return A UUID object.
     */
    public UUID getUUID(String path, UUID def) {
        UUID uuid = getUUID(path);
        return uuid != null ? uuid : def;
    }


    public List getList(String path) {
        return yamlFile.getList(path);
    }
    public List getList(String path, List<String> def) {
        return yamlFile.getList(path, def);
    }

    /**
     * Returns the StringList at the specified Path
     *
     * @param  path The path in the Config File
     * @return      The StringList at that Path
     */
    public List<String> getStringList(String path) {
        return yamlFile.getStringList(path);
    }
    public List<String> getStringList(String path, List<String> def) {
        if (contains(path)) return yamlFile.getStringList(path);
        return def;
    }

    // ------------------------------------------------------------------------------------------------------------------
    //      Basic Java Elements GetOrDefault
    // ------------------------------------------------------------------------------------------------------------------

    public <D> @NotNull List<D> getOrSetDefaultValue(@NotNull String path, @NotNull List<D> def) {
        if (!contains(path)){
            setValue(path, def);
            if (!newDefaultValueToSave) newDefaultValueToSave = true;
            return def;
        }else {
            if (def.size() > 0){
                Object firstValue = def.get(0);
                SmartLoadSave<D> smartLoadSave = CfgLoadableSalvable.getLoadableStatus(firstValue.getClass());
                if (smartLoadSave != null && smartLoadSave.isLoadable()){
                    return (List<D>) getLoadableList(path, def.get(0).getClass());
                }
            }

            return getList(path);
        }
    }

    public <D> @NotNull List<D> getOrSetDefaultValue(@NotNull String path, @NotNull List<D> def, @Nullable String comment) {
        List<D> theValue = getOrSetDefaultValue(path, def);
        String existingComment = getComment(path);
        if ((existingComment == null && comment != null) || (existingComment != null && !existingComment.equals(comment))){
            setComment(path, comment);
            newDefaultValueToSave = true;
        }
        return theValue;
    }


    public <D> @NotNull D getOrSetDefaultValue(@NotNull String path, @NotNull D def) {
        if (!contains(path)){
            setValue(path, def);
            if (!newDefaultValueToSave) newDefaultValueToSave = true;
            return def;
        }else {
            SmartLoadSave<D> smartLoadSave = CfgLoadableSalvable.getLoadableStatus(def.getClass());
            if (smartLoadSave != null && smartLoadSave.isLoadable()){
                return (D) getLoadable(path, def.getClass());
            }else {
                return (D) getValue(path);
            }
        }
    }

    public <D> @NotNull D getOrSetDefaultValue(@NotNull String path, @NotNull D def, @Nullable String comment) {
        D theValue = getOrSetDefaultValue(path, def);
        String existingComment = getComment(path);
        if ((existingComment == null && comment != null) || (existingComment != null && !existingComment.equals(comment))){
            setComment(path, comment);
            newDefaultValueToSave = true;
        }
        return theValue;
    }

    // ------------------------------------------------------------------------------------------------------------------
    //      Loadable System
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * If the path exists, get the loadable function for the class, and if it exists, apply it to the config section
     *
     * @param path The path to the config section you want to load.
     * @param loadableClass The class of the loadable you want to load.
     * @return A Loadable object or null
     */
    public <L> @Nullable L getLoadable(@NotNull String path, @NotNull Class<L> loadableClass) {
        Validate.notNull(loadableClass, "loadableClass cannot be null");
        if (contains(path)){
            SmartLoadSave<L> smartLoadSave = CfgLoadableSalvable.getLoadableStatus(loadableClass);
            if (smartLoadSave == null){
                throw new LoadableMethodException("Tried to load a non Loadable class [" + loadableClass.getName() + "]");
            }
            if (!smartLoadSave.isLoadable()){
                throw new LoadableMethodException("Tried to load an @Annotated class that has no onConfigLoad Method [" + loadableClass.getName() + "]");
            }
            return smartLoadSave.onConfigLoad(new ConfigSection(this, path));
        }
        return null;
    }

    /**
     * If the loadable at the given path is not null, return it, otherwise return the given default value.
     *
     * @param path The path to the value you want to get.
     * @param loadableDefault The default value to return if the path is not found.
     * @return The value of the path, or the default value if the path is not found.
     */
    public <L> @Nullable L getLoadable(@NotNull String path, @NotNull L loadableDefault) {
        Validate.notNull(loadableDefault, "loadableDefault cannot be null");
        L value = (L) getLoadable(path, loadableDefault.getClass());
        return value != null ? value : loadableDefault;
    }

    /**
     * It takes a path, and a class, and returns a list of the class, with each element being loaded from the config
     *
     * @param path The path to the list in the config
     * @param loadableClass The class that you want to load.
     * @return A list of loadable objects.
     */
    public <L> @NotNull List<L> getLoadableList(@NotNull String path, @NotNull Class<? extends L> loadableClass) {
        Validate.notNull(loadableClass, "loadableClass cannot be null");
        if (contains(path)){
            SmartLoadSave<L> smartLoadSave = CfgLoadableSalvable.getLoadableStatus(loadableClass);
            if (smartLoadSave == null){
                throw new LoadableMethodException("Tried to load a non Loadable class [" + loadableClass.getName() + "]");
            }

            if (!smartLoadSave.isLoadable()){
                throw new LoadableMethodException("Tried to load an @Annotated class that has no onConfigLoad Method [" + loadableClass.getName() + "]");
            }

            if (smartLoadSave.canSerializeToStringList()){//The stored value might be on a StringList
                return getStringList(path).stream().map(serializedLine -> smartLoadSave.onStringDeserialize(serializedLine)).collect(Collectors.toList());
            }

            List<L> loadableList = new ArrayList<>();
            for (String index : getKeys(path)) {
                L loadedValue = smartLoadSave.onConfigLoad(new ConfigSection(this, path + "." + index));
                loadableList.add(loadedValue);
            }
            return loadableList;
        }
        return new ArrayList<>();
    }

    /**
     * "If the value at the given path is a List of Loadables, return it, otherwise return the default value."
     *
     * @param path The path to the list in the config file.
     * @param loadableListDefault A list of the default values to be returned if the path is not found.
     * @return A List of Loadable Objects
     */
    public <L> @NotNull List<L> getLoadableList(@NotNull String path, @NotNull List<L> loadableListDefault) {
        Validate.notNull(loadableListDefault, "loadableListDefault cannot be null");
        if (loadableListDefault.size() == 0){
            throw new IllegalArgumentException("Cannot infer the Loadable class of an Empty Default List");
        }
        Object firstValue = loadableListDefault.get(0);

        List<L> result = (List<L>) getLoadableList(path, firstValue.getClass());
        return result.size() > 0 ? result : loadableListDefault;

    }
}
