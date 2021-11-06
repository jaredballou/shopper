package com.jballou.shopper.cache;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
//import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import com.jballou.shopper.records.BasicColor;

public class BlockSearchEntry {
  private final BlockState state;
  private final BasicColor color;
  private final boolean isDefault;

  BlockSearchEntry(BlockState state, BasicColor color, boolean isDefault) {
    this.state = state;
    this.color = color;
    this.isDefault = isDefault;
  }


  public BlockState getState() {
    return this.state;
  }

  public BasicColor getColor() {
    return this.color;
  }

  public boolean isDefault() {
    return this.isDefault;
  }

  // We don't care about the color and isDefault
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    BlockSearchEntry that = (BlockSearchEntry) o;
    return Objects.equal(this.state, that.state);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.state);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("state", this.state)
        .add("color", this.color)
        .add("isDefault", this.isDefault)
        .toString();
  }
}
