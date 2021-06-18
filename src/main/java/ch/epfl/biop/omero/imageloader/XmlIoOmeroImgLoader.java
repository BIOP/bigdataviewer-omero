package ch.epfl.biop.omero.imageloader;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import com.google.gson.Gson;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;


@ImgLoaderIo( format = "spimreconstruction.biop_omeroimageloader", type = OmeroImageLoader.class )
public class XmlIoOmeroImgLoader implements XmlIoBasicImgLoader<OmeroImageLoader> {

    public static final String OPENER_CLASS_TAG = "opener_class";
    public static final String OPENER_TAG = "opener";
    public static final String CACHE_NUM_FETCHER = "num_fetcher_threads";
    public static final String CACHE_NUM_PRIORITIES = "num_priorities";
    public static final String DATASET_NUMBER_TAG = "dataset_number";

    @Override
    public Element toXml(OmeroImageLoader imgLoader, File basePath) {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, this.getClass().getAnnotation( ImgLoaderIo.class ).format() );
        // For potential extensibility
        elem.addContent(XmlHelpers.textElement( OPENER_CLASS_TAG, OmeroSourceOpener.class.getName()));
        elem.addContent(XmlHelpers.intElement( CACHE_NUM_FETCHER, imgLoader.numFetcherThreads));
        elem.addContent(XmlHelpers.intElement( CACHE_NUM_PRIORITIES, imgLoader.numPriorities));
        elem.addContent(XmlHelpers.intElement( DATASET_NUMBER_TAG, imgLoader.openers.size()));

        Gson gson = new Gson();
        for (int i=0;i<imgLoader.openers.size();i++) {
            // Opener serialization
            elem.addContent(XmlHelpers.textElement(OPENER_TAG+"_"+i, gson.toJson(imgLoader.openers.get(i))));
        }
        return elem;
    }

    @Override
    public OmeroImageLoader fromXml(Element elem, File basePath, AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
        try
        {
            final int number_of_datasets = XmlHelpers.getInt( elem, DATASET_NUMBER_TAG );
            final int numFetcherThreads = XmlHelpers.getInt(elem, CACHE_NUM_FETCHER);
            final int numPriorities = XmlHelpers.getInt(elem, CACHE_NUM_PRIORITIES);

            List<OmeroSourceOpener> openers = new ArrayList<>();

            String openerClassName = XmlHelpers.getText( elem, OPENER_CLASS_TAG );

            if (!openerClassName.equals(OmeroSourceOpener.class.getName())) {
                throw new UnsupportedOperationException("Error class "+openerClassName+" not recognized.");
            }

            Gson gson = new Gson();
            for (int i=0;i<number_of_datasets;i++) {
                // Opener de-serialization
                String jsonInString = XmlHelpers.getText( elem, OPENER_TAG+"_"+i );
                OmeroSourceOpener opener = gson.fromJson(jsonInString, OmeroSourceOpener.class);
                openers.add(opener);
            }

            return new OmeroImageLoader(openers,sequenceDescription,numFetcherThreads, numPriorities);
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
