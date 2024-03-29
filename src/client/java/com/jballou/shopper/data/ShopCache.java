package com.jballou.shopper.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.jballou.shopper.Shopper;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

/**
 * The master cache structure.
 * This class is entirely static.
 * Tracks a lists of shops for each world.
 */
public final class ShopCache
{
	private static String REGEX_STR = ".*[\\\\/](.+)[\\\\/].+\\.dat";
	private static final Pattern REGEX_PATTERN = Pattern.compile(REGEX_STR);

	private static final HashMap<Identifier, WorldShopList> CACHE = new HashMap<>();

	private static class CacheSerializer implements JsonSerializer<ShopCache>
	{

		@Override
		public JsonElement serialize(ShopCache cache, Type typeOfSrc, JsonSerializationContext context)
		{
			JsonArray worlds = new JsonArray();

			for(Map.Entry<Identifier, WorldShopList> entry : CACHE.entrySet())
			{
				JsonObject world = new JsonObject();
				world.addProperty("id", entry.getKey().toString());
				world.add("signs", entry.getValue().toJson());
				worlds.add(world);
			}

			return worlds;
		}

	}

	private static class CacheDeserializer implements JsonDeserializer<ShopCache>
	{

		@Override
		public ShopCache deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException
		{
			JsonArray worlds = json.getAsJsonArray();
			for(JsonElement w : worlds.asList())
			{
				JsonObject world = w.getAsJsonObject();
				Identifier id = new Identifier(world.get("id").getAsString());
				CACHE.put(id, new WorldShopList(world.get("signs").getAsJsonArray(), id));
			}

			// also a little hacky, but whatever
			return new ShopCache();
		}
	}

	private ShopCache() {}

	public static void add(ShopSign sign)
	{
		WorldShopList list = CACHE.get(sign.dimension);
		if(list == null)
		{
			CACHE.put(sign.dimension, new WorldShopList());
			list = CACHE.get(sign.dimension);
		}

		list.add(sign);
	}

	public static void remove(ShopSign sign)
	{
		WorldShopList list = CACHE.get(sign.dimension);
		if(list != null)
		{
			list.remove(sign);
		}
	}

	public static void clear()
	{
		CACHE.clear();
	}

	/**
	 * Iterates through the WorldShopLists, finding the best value price from all
	 * possible Worlds
	 * @param itemName
	 * @param playerPos
	 * @return
	 */
	public static Pair<ShopSign, ShopSign> findBestPrices(String itemName, BlockPos playerPos)
	{
		Pair<ShopSign, ShopSign> result = new Pair<>(null, null);
		for (WorldShopList list : CACHE.values())
		{
			Pair<ShopSign, ShopSign> best = list.findBestPrices(itemName, playerPos);

			if(result.getLeft() == null)
			{
				//first iter
				result.setLeft(best.getLeft());
				result.setRight(best.getRight());
			}
			else
			{
				ShopSign bestBuyer = best.getLeft();
				ShopSign bestSeller = best.getRight();

				if(result.getLeft().buyPrice < bestBuyer.buyPrice)
				{
					result.setLeft(bestBuyer);
				}

				if(result.getRight().sellPrice > bestSeller.sellPrice)
				{
					result.setRight(bestSeller);
				}
			}
		}
		return result;
	}

	public static void populateShopSet(Set<ShopSign> set, String itemName)
	{
		for(WorldShopList shopList : CACHE.values())
		{
			shopList.populateShopSet(set, itemName);
		}
	}

	public static void joinListener(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client)
	{
		String fname = getFileNameForLevel(client);
		client.execute(() ->
		{
			clear();
			loadFromJson(fname);
		});
	}

	public static void disconnectListener(ClientPlayNetworkHandler handler, MinecraftClient client)
	{
		String fname = getFileNameForLevel(client);
		client.execute(() ->
		{
			saveToJson(fname);
		});
	}

	private static void loadFromJson(String fname)
	{
		Gson gson = new GsonBuilder()
			.registerTypeAdapter(ShopCache.class, new CacheDeserializer())
			.create();

		String path = String.format(Locale.ROOT, "%s/shops.%s.json", Shopper.CONFIG_PATH, fname);
		try
		{
			if(new File(path).exists())
			{
				FileReader reader = new FileReader(path);
				gson.fromJson(reader, ShopCache.class);
				reader.close();
				Shopper.LOG.info("Loaded Shopper cache from {}", path);
			}
			else
			{
				Shopper.LOG.info("No Shopper cache found for {}", path);
			}
		}
		catch(IOException | JsonParseException e)
		{
			Shopper.LOG.error("Could not read cache {}!", path);
			Shopper.LOG.error(e.getMessage());
		}

	}

	private static void saveToJson(String fname)
	{
		Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ShopCache.class, new CacheSerializer())
			.create();

		String path = String.format(Locale.ROOT, "%s/shops.%s.json", Shopper.CONFIG_PATH, fname);
		try
		{
			if(new File(Shopper.CONFIG_PATH).mkdirs())
			{
				Shopper.LOG.info("Created dir {}", Shopper.CONFIG_PATH);
			}

			FileWriter writer = new FileWriter(path);

			// new is a bit hacky here, not sure how to serialise a static class
			gson.toJson(new ShopCache(), writer);
			writer.flush();
			Shopper.LOG.info("Saved Shopper cache to {}", path);
			writer.close();
		}
		catch(IOException | JsonIOException e)
		{
			Shopper.LOG.error("Could not save cache {}!", path);
			Shopper.LOG.error(e.getMessage());
		}
	}

	private static String getFileNameForLevel(MinecraftClient client)
	{
		if(client.isConnectedToLocalServer())
		{
			// /run/media/crocomire/fabric/shopper/run/saves/New World (1)/level.dat
			Path p = client.getServer().getSavePath(WorldSavePath.LEVEL_DAT);
			Matcher matcher = REGEX_PATTERN.matcher(p.toString());
			if(matcher.matches())
			{
				return matcher.group(1);
			}
		}

		return client.getCurrentServerEntry().address;
	}
}
