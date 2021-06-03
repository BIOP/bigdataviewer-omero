package ch.epfl.biop.omero.omerosource;

import ch.epfl.biop.bdv.bioformats.bioformatssource.ResourcePool;
import omero.api.RawPixelsStorePrx;

import java.util.function.Supplier;

public class RawPixelsStorePool extends ResourcePool<RawPixelsStorePrx> {

    Supplier<RawPixelsStorePrx> rpsSupplier;

    public RawPixelsStorePool(int size, Boolean dynamicCreation, Supplier<RawPixelsStorePrx> rawPixelStoreSupplier) {
        super(size, dynamicCreation);
        createPool();
        this.rpsSupplier = rawPixelStoreSupplier;
    }

    @Override
    protected RawPixelsStorePrx createObject() {
        return rpsSupplier.get();
    }
}
