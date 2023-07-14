package com.jballou.shopper.data;

import com.jballou.shopper.ShopperClient;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;

public class ShopSign
{
	public String sellerName;
	public int amount;
	public int buyPrice;
	public int sellPrice;

	public ShopSign(SignBlockEntity sign, int buyPrice, int sellPrice, boolean useFrontSide)
	{
		SignText text = sign.getText(useFrontSide);
		sellerName = text.getMessage(0, false).getString();
		amount = Integer.parseInt(text.getMessage(1, false).getString());
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;

		ShopperClient.LOG.info("New sign: {} {} {} {}", sellerName, amount, buyPrice, sellPrice);
	}
}
