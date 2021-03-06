package mekanism.nei;

import java.util.Set;

import mekanism.client.GuiCrusher;
import mekanism.common.RecipeHandler.Recipe;

public class CrusherRecipeHandler extends MachineRecipeHandler
{
	@Override
	public String getRecipeName()
	{
		return "Crusher";
	}

	@Override
	public String getRecipeId()
	{
		return "mekanism.crusher";
	}
	
	@Override
	public String getOverlayIdentifier()
	{
		return "crusher";
	}

	@Override
	public Set getRecipes()
	{
		return Recipe.CRUSHER.get().entrySet();
	}

	@Override
	public String getGuiTexture()
	{
		return "/mods/mekanism/gui/GuiCrusher.png";
	}
	
	@Override
	public Class getGuiClass()
	{
		return GuiCrusher.class;
	}
}
