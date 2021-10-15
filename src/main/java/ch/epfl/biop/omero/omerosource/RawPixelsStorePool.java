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
