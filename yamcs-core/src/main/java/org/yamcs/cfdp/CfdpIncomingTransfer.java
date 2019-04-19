package org.yamcs.cfdp;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.cfdp.pdu.AckPacket;
import org.yamcs.cfdp.pdu.CfdpHeader;
import org.yamcs.cfdp.pdu.CfdpPacket;
import org.yamcs.cfdp.pdu.ConditionCode;
import org.yamcs.cfdp.pdu.FileDataPacket;
import org.yamcs.cfdp.pdu.FileDirectiveCode;
import org.yamcs.cfdp.pdu.FileStoreResponse;
import org.yamcs.cfdp.pdu.FinishedPacket;
import org.yamcs.cfdp.pdu.FinishedPacket.FileStatus;
import org.yamcs.cfdp.pdu.MetadataPacket;
import org.yamcs.cfdp.pdu.NakPacket;
import org.yamcs.cfdp.pdu.SegmentRequest;
import org.yamcs.protobuf.Cfdp.TransferState;
import org.yamcs.yarch.Stream;

public class CfdpIncomingTransfer extends CfdpTransaction {

    private static final Logger log = LoggerFactory.getLogger(CfdpIncomingTransfer.class);

    private enum CfdpTransferState {
        METADATA_RECEIVED,
        FILEDATA_RECEIVED,
        EOF_RECEIVED,
        RESENDING,
        FINISHED_SENT,
        FINISHED_ACK_RECEIVED
    }

    private CfdpTransferState currentState;
    private TransferState state;
    List<SegmentRequest> missingSegments;

    private DataFile incomingDataFile;

    public CfdpIncomingTransfer(MetadataPacket packet, Stream cfdpOut) {
        this(packet.getHeader().getSourceId(), cfdpOut);
        // create a new empty data file
        incomingDataFile = new DataFile(packet.getPacketLength());
        this.currentState = CfdpTransferState.METADATA_RECEIVED;
    }

    public CfdpIncomingTransfer(long initiatorEntity, Stream cfdpOut) {
        super(initiatorEntity, cfdpOut);
    }

    @Override
    public void step() {
        switch (currentState) {
        case METADATA_RECEIVED:
            // nothing to do, we're waiting for data
            state = TransferState.RUNNING;
            break;
        case FILEDATA_RECEIVED:
            // nothing to do, we're waiting for more data or an EOF
            // TODO, timeout + request resend
            break;
        case EOF_RECEIVED:
            // TODO must send FinishedPacket
            break;
        case FINISHED_SENT:
            // nothing to do, awaiting the ack
            break;
        case RESENDING:
            // nothing to do, waiting for further packets
            break;
        case FINISHED_ACK_RECEIVED:
            state = TransferState.COMPLETED;
            break;
        }
    }

    @Override
    public void processPacket(CfdpPacket packet) {
        if (packet.getHeader().isFileDirective()) {
            switch (((FileDirective) packet).getFileDirectiveCode()) {
            case EOF:
                // check completeness
                missingSegments = incomingDataFile.getMissingChunks();
                if (missingSegments.isEmpty()) {
                    sendFinishedPacket(packet);
                    this.currentState = CfdpTransferState.FINISHED_SENT;
                } else {
                    sendNakPacket(packet);
                    this.currentState = CfdpTransferState.RESENDING;
                }
                break;
            case ACK:
                AckPacket ack = (AckPacket) packet;
                if (ack.getFileDirectiveCode() == FileDirectiveCode.Finished) {
                    this.currentState = CfdpTransferState.FINISHED_ACK_RECEIVED;
                } else {
                    // TODO invalid ACK received
                }
                break;
            default:
                // TODO, unexpected packet
            }
        } else {
            FileDataPacket fdp = (FileDataPacket) packet;
            incomingDataFile.addSegment(new DataFileSegment(fdp.getOffset(), fdp.getData()));
            if (this.currentState == CfdpTransferState.RESENDING) {
                missingSegments.remove(new SegmentRequest(fdp.getOffset(), fdp.getOffset() + fdp.getData().length));
                log.info("RESENT file data received: " + new String(fdp.getData()).toString());
            }
            if (missingSegments.isEmpty()) {
                sendFinishedPacket(packet);
                this.currentState = CfdpTransferState.FINISHED_SENT;
            }
        }
    }

    private void sendNakPacket(CfdpPacket packet) {
        CfdpHeader header = new CfdpHeader(
                true, // file directive
                true, // towards sender
                false, // not acknowledged
                false, // no CRC
                packet.getHeader().getEntityIdLength(),
                packet.getHeader().getSequenceNumberLength(),
                packet.getHeader().getSourceId(),
                packet.getHeader().getDestinationId(),
                packet.getHeader().getSequenceNumber());

        NakPacket nak = new NakPacket(
                missingSegments.get(0).getSegmentStart(),
                missingSegments.get(missingSegments.size() - 1).getSegmentEnd(),
                missingSegments,
                header);
        sendPacket(nak);
    }

    private void sendFinishedPacket(CfdpPacket packet) {
        CfdpHeader header = new CfdpHeader(
                true, // file directive
                true, // towards sender
                false, // not acknowledged
                false, // no CRC
                packet.getHeader().getEntityIdLength(),
                packet.getHeader().getSequenceNumberLength(),
                packet.getHeader().getSourceId(),
                packet.getHeader().getDestinationId(),
                packet.getHeader().getSequenceNumber());

        FinishedPacket finished = new FinishedPacket(
                ConditionCode.NoError,
                true, // generated by end system
                false, // data complete
                FileStatus.SuccessfulRetention,
                new ArrayList<FileStoreResponse>(),
                null,
                header);

        sendPacket(finished);
    }
}