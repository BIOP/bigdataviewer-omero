package ch.epfl.biop.omero.imageloader;

import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.util.volatiles.SharedQueue;
import ch.epfl.biop.bdv.bioformats.imageloader.FileSerieChannel;
import ch.epfl.biop.omero.omerosource.OmeroSource;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import loci.formats.IFormatReader;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import net.imglib2.Volatile;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class OmeroImageLoader implements ViewerImgLoader, MultiResolutionImgLoader {

    public List<OmeroSourceOpener> openers;

    Map<Integer, OmeroIDChannel> viewSetupToBFFileSerieChannel = new HashMap<>();

    Map<Integer, NumericType> tTypeGetter = new HashMap<>();

    Map<Integer, Volatile> vTypeGetter = new HashMap<>();

    final AbstractSequenceDescription<?, ?, ?> sequenceDescription;

    HashMap<Integer, OmeroSetupLoader> imgLoaders = new HashMap<>();

    public Consumer<String> log = s -> {};//System.out.println(s);

    protected VolatileGlobalCellCache cache;

    protected SharedQueue cc;


    public final int numFetcherThreads;
    public final int numPriorities;

    public OmeroImageLoader(List<OmeroSourceOpener> openers, final AbstractSequenceDescription<?, ?, ?> sequenceDescription, int numFetcherThreads, int numPriorities) {
        this.openers = openers;
        this.sequenceDescription = sequenceDescription;
        this.numFetcherThreads=numFetcherThreads;
        this.numPriorities=numPriorities;
        cc = new SharedQueue(numFetcherThreads,numPriorities);

        openers.forEach(opener -> opener.setCache(cc));

        // NOT CORRECTLY IMPLEMENTED YET
        //final BlockingFetchQueues<Callable<?>> queue = new BlockingFetchQueues<>(1,1);
        cache = new VolatileGlobalCellCache(cc);
    }


    @Override
    public OmeroSetupLoader getSetupImgLoader(int setupId) {
        if (imgLoaders.containsKey(setupId)) {
            return imgLoaders.get(setupId);
        } else {
            int imageID = viewSetupToBFFileSerieChannel.get(setupId).OmeroID;
            int channel = viewSetupToBFFileSerieChannel.get(setupId).iChannel;
            log.accept("loading omero image number = "+imageID+" channel = "+channel+" setupId = "+setupId);

            OmeroSetupLoader imgL = null;
            try {
                imgL = new OmeroSetupLoader(
                        openers.get(imageID),
                        channel,
                        tTypeGetter.get(imageID),
                        vTypeGetter.get(imageID)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            imgLoaders.put(setupId,imgL);
            return imgL;
        }
    }

    @Override
    public CacheControl getCacheControl() {
        return cache;
    }
}
