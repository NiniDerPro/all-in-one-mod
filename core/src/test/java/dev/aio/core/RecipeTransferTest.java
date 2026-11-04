package dev.aio.core;

import dev.aio.core.modules.inventory.RecipeTransfer;
import dev.aio.core.platform.ContainerView;
import dev.aio.core.platform.ItemRef;
import dev.aio.core.platform.RecipeKind;
import dev.aio.core.platform.RecipeView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecipeTransferTest {
    private static ItemRef ref(String id) { return new FakeContainer.Stack(id, 1, 64); }

    /** Werkbank: Slots 0-8 = Raster, danach 36 Spielerslots. */
    private static FakeContainer craftingTable() {
        FakeContainer c = new FakeContainer(9);
        c.grid = new ContainerView.Grid(Set.of(RecipeKind.CRAFTING), 3, 3, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8});
        return c;
    }

    /** Spielerinventar mit 2×2: Slots 0-3 = Raster. */
    private static FakeContainer inventory2x2() {
        FakeContainer c = new FakeContainer(4);
        c.grid = new ContainerView.Grid(Set.of(RecipeKind.CRAFTING), 2, 2, new int[]{0, 1, 2, 3});
        return c;
    }

    /** Stöcke: 2 Bretter übereinander (1 breit, 2 hoch), beliebige Holzart. */
    private static RecipeView sticks() {
        List<ItemRef> planks = List.of(ref("oak_planks"), ref("birch_planks"));
        return new RecipeView("Werkbank", 1, 2, List.of(planks, planks), ref("stick"), ItemRef.EMPTY, RecipeKind.CRAFTING);
    }

    /** Werkbank-Block: 2×2 Bretter. */
    private static RecipeView table() {
        List<ItemRef> p = List.of(ref("oak_planks"));
        return new RecipeView("Werkbank", 2, 2, List.of(p, p, p, p), ref("crafting_table"), ItemRef.EMPTY, RecipeKind.CRAFTING);
    }

    /** Ofen: 3×3 Bruchstein mit leerer Mitte. */
    private static RecipeView furnace() {
        List<ItemRef> s = List.of(ref("cobblestone"));
        return new RecipeView("Werkbank", 3, 3, List.of(s, s, s, s, List.of(), s, s, s, s), ref("furnace"), ItemRef.EMPTY, RecipeKind.CRAFTING);
    }

    @Test void placesOnceIntoCraftingTable() {
        FakeContainer c = craftingTable();
        c.set(20, "cobblestone", 20);
        assertEquals(RecipeTransfer.Status.OK, RecipeTransfer.check(c, furnace()));
        assertEquals(1, RecipeTransfer.transfer(c, furnace(), false));
        for (int i = 0; i < 9; i++) {
            if (i == 4) assertTrue(c.get(i).isEmpty(), "Mitte bleibt leer");
            else assertEquals(1, c.get(i).count());
        }
        assertEquals(20, c.total("cobblestone"), "nichts verloren");
        assertTrue(c.cursor().isEmpty());
    }

    @Test void shiftPlacesAsManyAsPossible() {
        FakeContainer c = inventory2x2();
        c.set(10, "oak_planks", 10);
        assertEquals(2, RecipeTransfer.transfer(c, table(), true), "10 Bretter reichen für 2× (je 4)");
        for (int i = 0; i < 4; i++) assertEquals(2, c.get(i).count());
        assertEquals(10, c.total("oak_planks"));
    }

    @Test void smallRecipeFitsInto2x2ButBigDoesNot() {
        FakeContainer c = inventory2x2();
        c.set(10, "birch_planks", 5);
        c.set(11, "cobblestone", 20);
        assertEquals(RecipeTransfer.Status.OK, RecipeTransfer.check(c, sticks()));
        assertEquals(RecipeTransfer.Status.TOO_BIG, RecipeTransfer.check(c, furnace()));
        RecipeTransfer.transfer(c, sticks(), false);
        assertEquals("birch_planks", c.get(0).id(), "Tag-Alternative wird genommen");
        assertEquals("birch_planks", c.get(2).id(), "zweite Zeile im 2×2 = Slot 2");
    }

    @Test void missingItemsAndWrongStation() {
        FakeContainer c = craftingTable();
        c.set(15, "cobblestone", 3);
        assertEquals(RecipeTransfer.Status.MISSING_ITEMS, RecipeTransfer.check(c, furnace()));
        assertEquals(5, RecipeTransfer.missing(c, furnace()).size());
        assertEquals(0, RecipeTransfer.transfer(c, furnace(), false));
        RecipeView smelt = new RecipeView("Ofen", 1, 1, List.of(List.of(ref("iron_ore"))), ref("iron_ingot"), ItemRef.EMPTY, RecipeKind.FURNACE);
        assertEquals(RecipeTransfer.Status.WRONG_STATION, RecipeTransfer.check(c, smelt));
        assertEquals(RecipeTransfer.Status.WRONG_STATION, RecipeTransfer.check(null, smelt));
    }

    @Test void furnaceInput() {
        FakeContainer c = new FakeContainer(3);
        c.grid = new ContainerView.Grid(Set.of(RecipeKind.FURNACE), 1, 1, new int[]{0});
        c.set(30, "iron_ore", 40);
        RecipeView smelt = new RecipeView("Ofen", 1, 1, List.of(List.of(ref("iron_ore"))), ref("iron_ingot"), ItemRef.EMPTY, RecipeKind.FURNACE);
        assertEquals(40, RecipeTransfer.transfer(c, smelt, true), "Shift füllt den Eingang");
        assertEquals(40, c.get(0).count());
    }

    @Test void clearsOldGridContentFirst() {
        FakeContainer c = inventory2x2();
        c.set(0, "dirt", 3); // altes Zeug im Raster
        c.set(10, "oak_planks", 4);
        assertEquals(1, RecipeTransfer.transfer(c, table(), false));
        assertEquals(3, c.total("dirt"), "Erde zurück ins Inventar, nicht weg");
        for (int i = 0; i < 4; i++) assertEquals("oak_planks", c.get(i).id());
    }
}
