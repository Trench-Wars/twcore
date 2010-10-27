package twcore.bots.elimbot.elimsystem.testcases;

import static org.junit.Assert.*;

import org.junit.Test;

import twcore.bots.elimbot.elimsystem.ElimSystem;

/**
 * J-unit case tests
 * @author quiles
 * */
public class ElimSystemTest {

    @Test
    public void testComparisonAlg(){
        ElimSystem system = new ElimSystem();
        int vecTest[] =  {10, 100, 20, 2, 6, 5};
        int max[] = system.getMax(vecTest, 0, 5);
        assertEquals(100, max[1]); //max element
        assertEquals(1, max[0]);//position of max element
    }
    
    
}
