import ch.epfl.biop.ij2command.OmeroTools;
import ch.epfl.biop.utils.SplitOmeroURL;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import static ch.epfl.biop.ij2command.OmeroTools.getSecurityContext;

public class TestURL {

    @Test
    public void testOmeroURL1() throws Exception {
        // Case single image link, generated with the CREATE LINK BUTTON  in OMERO.web
        // Example: https://omero-poc.epfl.ch/webclient/?show=image-4732
        String omeroURL = "https://omero-poc.epfl.ch/webclient/?show=image-4732";
        int[] omeroIDs = {4732};
        testOmeroURL(omeroURL, true, omeroIDs);
    }
    @Test
    public void testOmeroURL2() throws Exception {
        // Case multiple images link, generated with the CREATE LINK BUTTON  in OMERO.web
        // Example: https://omero-poc.epfl.ch/webclient/?show=image-24602|image-24603|image-24604|image-24605|image-24606
        String omeroURL = "http://omero-poc.epfl.ch/webclient/?show=image-24602|image-24603|image-24604|image-24605|image-24606";
        int[] omeroIDs = IntStream.rangeClosed(24602, 24606).toArray();
        testOmeroURL(omeroURL, true, omeroIDs);
    }

    @Test
    public void testOmeroURL3() throws Exception {
        // Case single dataset link, generated with the CREATE LINK BUTTON  in OMERO.web
        // Example: https://omero-poc.epfl.ch/webclient/?show=dataset-604
        String omeroURL = "https://omero-poc.epfl.ch/webclient/?show=dataset-604";
        int[] omeroIDs = IntStream.rangeClosed(4732, 4745).toArray();
        testOmeroURL(omeroURL, false, omeroIDs);
    }

    @Test
    public void testOmeroURL4() throws Exception {
        // Case multiple datasets link, generated with the CREATE LINK BUTTON  in OMERO.web
        // Example: https://omero-poc.epfl.ch/webclient/?show=dataset-604|dataset-603
        String omeroURL = "https://omero-poc.epfl.ch/webclient/?show=dataset-604|dataset-603";
        int[] omeroIDDataset1 = IntStream.rangeClosed(4732, 4745).toArray();
        int[] omeroIDDataset2 =  {4727};
        int[] omeroIDs = ArrayUtils.addAll(omeroIDDataset1, omeroIDDataset2);
        testOmeroURL(omeroURL, false, omeroIDs);
    }

    @Test
    public void testOmeroURL5() throws Exception {
        // Case single image link, pasted from iviewer (iviewer opened with a double clic on a thumbnail)
        // Example: https://omero-poc.epfl.ch/webclient/img_detail/4732/?dataset=604
        String omeroURL = "https://omero-poc.epfl.ch/webclient/img_detail/4732/?dataset=604";
        int[] omeroIDs = {4732};
        testOmeroURL(omeroURL, true, omeroIDs);
    }

    @Test
    public void testOmeroURL6() throws Exception {
        // Case single image link, pasted from iviewer (iviewer opened with the open with button)
        // Example: https://omero-poc.epfl.ch/iviewer/?images=4735&dataset=604
        String omeroURL = "https://omero-poc.epfl.ch/iviewer/?images=4735&dataset=604";
        int[] omeroIDs = {4735};
        testOmeroURL(omeroURL, true, omeroIDs);
    }

    @Test
    public void testOmeroURL7() throws Exception {
        // Case multiple images link, pasted from iviewer (iviewer opened with the open with button)
        // Example: https://omero-poc.epfl.ch/iviewer/?images=4732,4733,4734,4735
        String omeroURL = "https://omero-poc.epfl.ch/iviewer/?images=4732,4733,4734,4735";
        int[] omeroIDs = IntStream.rangeClosed(4732, 4735).toArray();
        testOmeroURL(omeroURL, true, omeroIDs);
    }



    public void testOmeroURL(String omeroURL, boolean order, int... omeroIDs) throws Exception {
        int port = 4064;
        String username = "demo";
        String password = "demotraining";

        // Act
        SplitOmeroURL splitOmeroURL = new SplitOmeroURL();
        splitOmeroURL.setHost(omeroURL);
        Assert.assertEquals("omero-poc.epfl.ch", splitOmeroURL.host);
        Gateway gateway =  OmeroTools.omeroConnect(splitOmeroURL.host, port, username, password);
        System.out.println( "Session active : "+gateway.isConnected() );
        SecurityContext ctx = getSecurityContext(gateway);
        splitOmeroURL.setImageIDs(omeroURL,gateway,ctx);
        List<Long> expectedImageIDs = getListOf(omeroIDs);
        // Assert
        if(order){
            Assert.assertEquals(expectedImageIDs, splitOmeroURL.imageIDs);
        } else {
            boolean test = true;
            for (int i=0; i<expectedImageIDs.size(); i++){
                if ( !splitOmeroURL.imageIDs.contains(expectedImageIDs.get(i))){
                    test = false;
                }
            }
            Assert.assertTrue(test);
        }

    }



    public static List<Long> getListOf(int... nombres) {
        List<Long> imageIDs = new ArrayList<>();
        for(int n:nombres) {
            imageIDs.add(Long.valueOf(n));
        }
        return imageIDs;
    }


    static <T> T[] concatWithCollection(T[] array1, T[] array2) {
        List<T> resultList = new ArrayList<>(array1.length + array2.length);
        Collections.addAll(resultList, array1);
        Collections.addAll(resultList, array2);

        @SuppressWarnings("unchecked")
        //the type cast is safe as the array1 has the type T[]
        T[] resultArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), 0);
        return resultList.toArray(resultArray);
    }
}
