package ch.epfl.biop.omero.omerosource;

import ch.epfl.biop.bdv.bioformats.bioformatssource.ResourcePool;
import loci.formats.IFormatReader;
import omero.api.RawPixelsStorePrx;

import java.util.function.Supplier;

public class RawPixelsStorePool extends ResourcePool<RawPixelsStorePrx> {

    Supplier<RawPixelsStorePrx> rpsSupplier;

    public RawPixelsStorePool(int size, Boolean dynamicCreation, Supplier<RawPixelsStorePrx> readerSupplier) {
        super(size, dynamicCreation);
        createPool();
        this.rpsSupplier = readerSupplier;
    }

    @Override
    protected RawPixelsStorePrx createObject() {
        return rpsSupplier.get();
    }
}
