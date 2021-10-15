/*-
 * #%L
 * A nice project implementing an OMERO connection with ImageJ
 * %%
 * Copyright (C) 2021 EPFL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package ch.epfl.biop.omero.imageloader;

import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.util.volatiles.SharedQueue;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import net.imglib2.Volatile;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class OmeroImageLoader implements ViewerImgLoader, MultiResolutionImgLoader {

    public List<OmeroSourceOpener> openers;

    Map<Integer, OpenerIdxChannel> viewSetupToOpenerIdxChannel = new HashMap<>();

    Map<Integer, NumericType> tTypeGetter = new HashMap<>();

    Map<Integer, Volatile> vTypeGetter = new HashMap<>();

    final AbstractSequenceDescription<?, ?, ?> sequenceDescription;

    HashMap<Integer, OmeroSetupLoader> imgLoaders = new HashMap<>();

    public Consumer<String> log = s -> {};//System.out.println(s);

    protected VolatileGlobalCellCache cache;

    protected SharedQueue cc;

    public final int numFetcherThreads;
    public final int numPriorities;

    /**
     * OMERO image loader constructor
     * @param openers
     * @param sequenceDescription
     * @param numFetcherThreads
     * @param numPriorities
     * @throws Exception
     */
    public OmeroImageLoader(List<OmeroSourceOpener> openers, final AbstractSequenceDescription<?, ?, ?> sequenceDescription, int numFetcherThreads, int numPriorities) throws Exception {
        this.openers = openers;
        this.sequenceDescription = sequenceDescription;
        this.numFetcherThreads=numFetcherThreads;
        this.numPriorities=numPriorities;
        cc = new SharedQueue(numFetcherThreads,numPriorities);

        openers.forEach(opener -> opener.setCache(cc));

        int viewSetupCounter = 0;
        if ((sequenceDescription!=null)) {
            //openersIdxStream.forEach(openerIdx -> {
            for (int openerIdx=0; openerIdx<openers.size(); openerIdx++){
                OmeroSourceOpener opener = openers.get(openerIdx);
                // Register Setups (one per channel and one per timepoint)
                for (int channelIdx=0; channelIdx<opener.getSizeC(); channelIdx++){
                    OpenerIdxChannel openerIdxChannel = new OpenerIdxChannel(openerIdx,channelIdx);
                    viewSetupToOpenerIdxChannel.put(viewSetupCounter,openerIdxChannel);
                    Type t = opener.getNumericType(0);
                    tTypeGetter.put(viewSetupCounter,(NumericType)t);
                    Volatile v =  BioFormatsBdvSource.getVolatileOf((NumericType)t);
                    vTypeGetter.put(viewSetupCounter, v);
                    viewSetupCounter++;
                }
            }
        }

        // NOT CORRECTLY IMPLEMENTED YET
        //final BlockingFetchQueues<Callable<?>> queue = new BlockingFetchQueues<>(1,1);
        cache = new VolatileGlobalCellCache(cc);
    }


    @Override
    public OmeroSetupLoader getSetupImgLoader(int setupId) {
        if (imgLoaders.containsKey(setupId)) {
            return imgLoaders.get(setupId);
        } else {
            int openerIdx = viewSetupToOpenerIdxChannel.get(setupId).openerIdx;
            int channel = viewSetupToOpenerIdxChannel.get(setupId).iChannel;

            OmeroSetupLoader imgL = null;
            try {
                imgL = new OmeroSetupLoader(
                        openers.get(openerIdx),
                        channel,
                        tTypeGetter.get(setupId),
                        vTypeGetter.get(setupId)
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
