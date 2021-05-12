package no.hvl.tk.visualDebugger.partsList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartsListTest {

    @Test
    void overallCostForFoldingWallTableTest() {
        final Product folding_wall_table = Product.create("Folding wall table", 5);
        folding_wall_table.addPart(Material.create("Main support", 10), 1);
        folding_wall_table.addPart(Material.create("Hinge", 5), 4);
        folding_wall_table.addPart(Material.create("Wood screw D3,5 x 20mm", 1), 26);
        folding_wall_table.addPart(Material.create("Wood screw D4 x 45mm", 1), 10);
        // Real cost is 71. Forgot the assemblyCost of 5.
        assertEquals(71, folding_wall_table.getOverallCost());
    }
}
