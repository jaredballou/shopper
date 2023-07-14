package com.jballou.shopper.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

public final class ShopList
{
	private static class ItemList
	{
		public final List<ShopSign> signs = new ArrayList<>();

		public ShopSign add(ShopSign sign)
		{
			signs.add(sign);
			return sign;
		}

		public ShopSign remove(ShopSign sign)
		{
			signs.remove(sign);
			return sign;
		}
	}

	private final Map<BlockPos, ShopSign> SIGNS = new HashMap<>();
	private final Map<String, ItemList> ITEMS = new HashMap<>();

	public ShopList() {}

	public void add(ShopSign sign)
	{
		ShopSign oldSign = SIGNS.get(sign.pos);
		if(oldSign != null && !sign.compare(oldSign))
		{
			SIGNS.replace(sign.pos, sign);
			remove(oldSign);
		}

		String name = sign.getComparableItemName();
		if(!ITEMS.containsKey(name))
		{
			ITEMS.put(name, new ItemList());
		}
		ITEMS.get(name).add(sign);
	}

	public void remove(ShopSign sign)
	{
		SIGNS.remove(sign.pos);
		ITEMS.get(sign.getComparableItemName()).remove(sign);
	}

	public Pair<ShopSign, ShopSign> findBestPrices(String itemName)
	{
		Pair<ShopSign, ShopSign> result = new Pair<>(null, null);
		ItemList list = ITEMS.get(itemName.toLowerCase(Locale.ROOT));
		
		if(list == null || list.signs.size() == 0)
		{
			return result;
		}

		ShopSign bestBuyer = list.signs.get(0);
		ShopSign bestSeller = list.signs.get(0);
		for(ShopSign sign : list.signs)
		{
			bestBuyer = sign.buyPrice < bestBuyer.buyPrice ? sign : bestBuyer;
			bestSeller = sign.sellPrice > bestSeller.sellPrice ? sign : bestSeller;
		}

		result.setLeft(bestBuyer);
		result.setRight(bestSeller);
		return result;
	}

	private void loadJson()
	{

	}

	private void saveJson()
	{

	}
}