package com.mesosphere.sdk.offer;

import com.mesosphere.sdk.testutils.OfferTestUtils;
import com.mesosphere.sdk.testutils.ResourceTestUtils;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class MesosResourcePoolTest {

    @Test
    public void testEmptyUnreservedAtomicPool() {
        Offer offer = OfferTestUtils.getOffer(ResourceTestUtils.getUnreservedCpu(1.0));
        MesosResourcePool pool = new MesosResourcePool(offer);

        Assert.assertEquals(0, pool.getUnreservedAtomicPool().size());
    }

    @Test
    public void testCreateSingleUnreservedAtomicPool() {
        Offer offer = OfferTestUtils.getOffer(ResourceTestUtils.getUnreservedMountVolume(1000));
        MesosResourcePool pool = new MesosResourcePool(offer);

        Assert.assertEquals(1, pool.getUnreservedAtomicPool().size());
        Assert.assertEquals(1, pool.getUnreservedAtomicPool().get("disk").size());
    }

    @Test
    public void testCreateSingleReservedAtomicPool() {
        Resource resource = ResourceTestUtils.getExpectedMountVolume(1000);
        Offer offer = OfferTestUtils.getOffer(resource);
        MesosResourcePool pool = new MesosResourcePool(offer);
        String resourceId = new MesosResource(resource).getResourceId().get();

        Assert.assertEquals(0, pool.getUnreservedAtomicPool().size());
        Assert.assertEquals(1, pool.getReservedPool().size());
        Assert.assertEquals(resource, pool.getReservedPool().get(resourceId).getResource());
    }

    @Test
    public void testMultipleUnreservedAtomicPool() {
        Resource resource = ResourceTestUtils.getUnreservedMountVolume(1000);
        Offer offer = OfferTestUtils.getOffer(Arrays.asList(resource, resource));
        MesosResourcePool pool = new MesosResourcePool(offer);

        Assert.assertEquals(1, pool.getUnreservedAtomicPool().size());
        Assert.assertEquals(2, pool.getUnreservedAtomicPool().get("disk").size());
    }

    @Test
    public void testConsumeUnreservedAtomicResource() {
        Resource offerResource = ResourceTestUtils.getUnreservedMountVolume(1000);
        Resource resource = ResourceTestUtils.getDesiredMountVolume(1000);
        Protos.Value resourceValue = ValueUtils.getValue(resource);
        Offer offer = OfferTestUtils.getOffer(offerResource);
        MesosResourcePool pool = new MesosResourcePool(offer);

        Assert.assertEquals(1, pool.getUnreservedAtomicPool().size());
        MesosResource resourceToConsume = pool.consumeAtomic(resource.getName(), resourceValue).get();
        Assert.assertEquals(offerResource, resourceToConsume.getResource());
        Assert.assertEquals(0, pool.getUnreservedAtomicPool().size());
    }

    @Test
    public void testConsumeReservedMergedResource() {
        Resource resource = ResourceTestUtils.getExpectedCpu(1.0);
        Protos.Value resourceValue = ValueUtils.getValue(resource);
        String resourceId = ResourceTestUtils.getResourceId(resource);
        Offer offer = OfferTestUtils.getOffer(resource);
        MesosResourcePool pool = new MesosResourcePool(offer);

        Assert.assertEquals(1, pool.getReservedPool().size());
        MesosResource resourceToConsume = pool.consumeReserved(resource.getName(), resourceValue, resourceId).get();
        Assert.assertEquals(resource, resourceToConsume.getResource());
        Assert.assertEquals(0, pool.getReservedPool().size());
    }

    @Test
    public void testConsumeUnreservedMergedResource() {
        Resource resource = ResourceTestUtils.getUnreservedCpu(1.0);
        Protos.Value resourceValue = ValueUtils.getValue(resource);
        Offer offer = OfferTestUtils.getOffer(resource);
        MesosResourcePool pool = new MesosResourcePool(offer);

        Assert.assertEquals(1, pool.getUnreservedMergedPool().size());
        Assert.assertEquals(resource.getScalar().getValue(),
                pool.getUnreservedMergedPool().get("cpus").getScalar().getValue(), 0.0);
        MesosResource resourceToConsume = pool.consumeUnreservedMerged(resource.getName(), resourceValue).get();
        Assert.assertEquals(resource, resourceToConsume.getResource());
        Assert.assertEquals(ValueUtils.getZero(Protos.Value.Type.SCALAR),
                pool.getUnreservedMergedPool().get("cpus"));
    }

    @Test
    public void testConsumeInsufficientUnreservedMergedResource() {
        Resource desiredUnreservedResource = ResourceTestUtils.getUnreservedCpu(2.0);
        Protos.Value resourceValue = ValueUtils.getValue(desiredUnreservedResource);
        Resource offeredUnreservedResource = ResourceTestUtils.getUnreservedScalar("cpus", 1.0);
        Offer offer = OfferTestUtils.getOffer(offeredUnreservedResource);
        MesosResourcePool pool = new MesosResourcePool(offer);

        Assert.assertFalse(
                pool.consumeUnreservedMerged(
                        desiredUnreservedResource.getName(),
                        resourceValue)
                        .isPresent());
    }
}