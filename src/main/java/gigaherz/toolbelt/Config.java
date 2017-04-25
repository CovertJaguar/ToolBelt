package gigaherz.toolbelt;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import gigaherz.toolbelt.belt.ItemToolBelt;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Config
{
    private static final Set<ItemStack> blackList = Sets.newHashSet();
    private static final Set<ItemStack> whiteList = Sets.newHashSet();

    public static boolean showBeltOnPlayers = true;
    public static boolean showOwnBelt = true;

    public static ConfigCategory display;

    static void loadConfig(Configuration config)
    {
        Property bl = config.get("tileEntities", "blacklist", new String[0]);
        bl.setComment("List of items to disallow from placing in the belt.");

        Property wl = config.get("tileEntities", "whitelist", new String[0]);
        wl.setComment("List of items to force-allow placing in the belt. Takes precedence over blacklist.");

        Property showBeltOnPlayersProperty = config.get("display", "showBeltOnPlayers", true);
        showBeltOnPlayersProperty.setComment("If set to FALSE, the belts and tools will NOT draw on players.");

        display = config.getCategory("display");
        display.setComment("Options for customizing the display of tools on the player");

        showBeltOnPlayers = showBeltOnPlayersProperty.getBoolean();

        blackList.addAll(Arrays.stream(bl.getStringList()).map(Config::parseItemStack).filter(Objects::nonNull).collect(Collectors.toList()));
        whiteList.addAll(Arrays.stream(wl.getStringList()).map(Config::parseItemStack).filter(Objects::nonNull).collect(Collectors.toList()));
        if (!bl.wasRead() || !wl.wasRead() || !showBeltOnPlayersProperty.wasRead())
            config.save();
    }

    public static void refresh()
    {
        showBeltOnPlayers = display.get("showBeltOnPlayers").getBoolean();
    }

    private static final Pattern itemRegex = Pattern.compile("^(?<item>([a-zA-Z-0-9_]+:)?[a-zA-Z-0-9_]+)(?:@((?<meta>[0-9]+)|(?<any>any)))?$");

    private static ItemStack parseItemStack(String itemString)
    {
        Matcher matcher = itemRegex.matcher(itemString);

        if (!matcher.matches())
        {
            ToolBelt.logger.warn("Could not parse item " + itemString);
            return ItemStack.EMPTY;
        }

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(matcher.group("item")));
        if (item == null)
        {
            ToolBelt.logger.warn("Could not parse item " + itemString);
            return ItemStack.EMPTY;
        }

        String anyString = matcher.group("meta");
        String metaString = matcher.group("meta");
        int meta = Strings.isNullOrEmpty(anyString)
                ? (Strings.isNullOrEmpty(metaString) ? 0 : Integer.parseInt(metaString))
                : OreDictionary.WILDCARD_VALUE;

        return new ItemStack(item, 1, meta);
    }

    public static boolean isItemStackAllowed(final ItemStack stack)
    {
        if (stack.getCount() <= 0)
            return true;

        if (whiteList.stream().anyMatch((s) -> OreDictionary.itemMatches(s, stack, false)))
            return true;

        if (blackList.stream().anyMatch((s) -> OreDictionary.itemMatches(s, stack, false)))
            return false;

        if (stack.getItem() instanceof ItemToolBelt)
            return false;

        if (stack.getMaxStackSize() != 1)
            return false;

        return true;
    }
}
