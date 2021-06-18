package ch.epfl.biop.omero.imageloader;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.bdv.bioformats.imageloader.BioFormatsSetupLoader;
import ch.epfl.biop.bdv.bioformats.imageloader.FileSerieChannel;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.NumericType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OmeroImageLoader implements ViewerImgLoader, MultiResolutionImgLoader {

    public List<OmeroSourceOpener> openers;

    Map<Integer, OmeroIDChannel> viewSetupToBFFileSerieChannel = new HashMap<>();

    Map<Integer, NumericType> tTypeGetter = new HashMap<>();

    Map<Integer, Volatile> vTypeGetter = new HashMap<>();


    HashMap<Integer, OmeroSetupLoader> imgLoaders = new HashMap<>();

    public Consumer<String> log = s -> {};//System.out.println(s);



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
        return null;
    }
}
