/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.services.telephony;

/// M: For VoLTE enhanced conference call. @{
import android.content.Context;
/// @}

import android.net.Uri;
import android.telecom.Conference;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
/// M: For VoLTE enhanced conference call. @{
import android.telephony.PhoneNumberUtils;
/// M: For query local MSISDN.
import android.telephony.TelephonyManager;
import android.widget.Toast;
/// @}

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.PhoneUtils;

/// M: For VoLTE enhanced conference call. @{
import com.android.phone.R;
/// @}

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an IMS conference call.
 * <p>
 * An IMS conference call consists of a conference host connection and potentially a list of
 * conference participants.  The conference host connection represents the radio connection to the
 * IMS conference server.  Since it is not a connection to any one individual, it is not represented
 * in Telecom/InCall as a call.  The conference participant information is received via the host
 * connection via a conference event package.  Conference participant connections do not represent
 * actual radio connections to the participants; they act as a virtual representation of the
 * participant, keyed by a unique endpoint(MTK: we should use user entity) {@link android.net.Uri}.
 * <p>
 * The {@link ImsConference} listens for conference event package data received via the host
 * connection and is responsible for managing the conference participant connections which represent
 * the participants.
 */
public class ImsConference extends Conference {

    /**
     * Listener used to respond to changes to conference participants.  At the conference level we
     * are most concerned with handling destruction of a conference participant.
     */
    private final Connection.Listener mParticipantListener = new Connection.Listener() {
        /**
         * Participant has been destroyed.  Remove it from the conference.
         *
         * @param connection The participant which was destroyed.
         */
        @Override
        public void onDestroyed(Connection connection) {
            ConferenceParticipantConnection participant =
                    (ConferenceParticipantConnection) connection;
            removeConferenceParticipant(participant);
            updateManageConference();
        }

    };

    /**
     * Listener used to respond to changes to the underlying radio connection for the conference
     * host connection.  Used to respond to SRVCC changes.
     */
    private final TelephonyConnection.TelephonyConnectionListener mTelephonyConnectionListener =
            new TelephonyConnection.TelephonyConnectionListener() {

        @Override
        public void onOriginalConnectionConfigured(TelephonyConnection c) {
            if (c == mConferenceHost) {
               handleOriginalConnectionChange();
            }
        }

        /// M: VoLTE. @{
        /**
         * For VoLTE enhanced conference call, notify invite conf. participants completed.
         * @param isSuccess is success or not.
         */
        @Override
        public void onConferenceParticipantsInvited(boolean isSuccess) {
            mIsDuringAddingParticipants = false;
        }

        /**
         * For VoLTE conference SRVCC, notify when new participant connections maded.
         * @param radioConnections new participant connections.
         */
        @Override
        public void onConferenceConnectionsConfigured(
                ArrayList<com.android.internal.telephony.Connection> radioConnections) {
            handleConferenceSRVCC(radioConnections);
        }
        /// @}
    };

    /**
     * Listener used to respond to changes to the connection to the IMS conference server.
     */
    private final android.telecom.Connection.Listener mConferenceHostListener =
            new android.telecom.Connection.Listener() {

        /**
         * Updates the state of the conference based on the new state of the host.
         *
         * @param c The host connection.
         * @param state The new state
         */
        @Override
        public void onStateChanged(android.telecom.Connection c, int state) {
            setState(state);
        }

        /**
         * Disconnects the conference when its host connection disconnects.
         *
         * @param c The host connection.
         * @param disconnectCause The host connection disconnect cause.
         */
        @Override
        public void onDisconnected(android.telecom.Connection c, DisconnectCause disconnectCause) {
            setDisconnected(disconnectCause);
        }

        /**
         * Handles destruction of the host connection; once the host connection has been
         * destroyed, cleans up the conference participant connection.
         *
         * @param connection The host connection.
         */
        @Override
        public void onDestroyed(android.telecom.Connection connection) {
            disconnectConferenceParticipants();
        }

        /**
         * Handles changes to conference participant data as reported by the conference host
         * connection.
         *
         * @param c The connection.
         * @param participants The participant information.
         */
        @Override
        public void onConferenceParticipantsChanged(android.telecom.Connection c,
                List<ConferenceParticipant> participants) {

            if (c == null) {
                return;
            }
            Log.v(this, "onConferenceParticipantsChanged: %d participants", participants.size());
            TelephonyConnection telephonyConnection = (TelephonyConnection) c;
            handleConferenceParticipantsUpdate(telephonyConnection, participants);
        }
    };

    /**
     * The telephony connection service; used to add new participant connections to Telecom.
     */
    private TelephonyConnectionService mTelephonyConnectionService;

    /**
     * The connection to the conference server which is hosting the conference.
     */
    private TelephonyConnection mConferenceHost;

    /**
     * The known conference participant connections.  The HashMap is keyed by endpoint Uri.
     * (MTK: according to ts_124147 and rfc4575, we should use user entity Uri as the key.)
     * A {@link ConcurrentHashMap} is used as there is a possibility for radio events impacting the
     * available participants to occur at the same time as an access via the connection service.
     */
    private final ConcurrentHashMap<Uri, ConferenceParticipantConnection>
            mConferenceParticipantConnections =
                    new ConcurrentHashMap<Uri, ConferenceParticipantConnection>(8, 0.9f, 1);

    /// M: For VoLTE enhanced conference call. @{
    private boolean mIsDuringAddingParticipants = false;
    /// @}

    /// M: ALPS02209724. Filter host call in manage conference screen, and support addMember. @{
    private Uri mHostCallAddress = null;

    /**
     * Max. numbers of participants in a IMS conference.
     */
    public static final int IMS_CONFERENCE_MAX_SIZE = 5;
    /// @}

    /// M: ALPS02487069. Keep the state of conference host. @{
    private int mHostCallState = Connection.STATE_NEW;
    /// @}

    /**
     * Initializes a new {@link ImsConference}.
     *
     * @param telephonyConnectionService The connection service responsible for adding new
     *                                   conferene participants.
     * @param conferenceHost The telephony connection hosting the conference.
     */
    public ImsConference(TelephonyConnectionService telephonyConnectionService,
            TelephonyConnection conferenceHost) {

        super(null);

        // Specify the connection time of the conference to be the connection time of the original
        // connection.
        setConnectTimeMillis(conferenceHost.getOriginalConnection().getConnectTime());

        mTelephonyConnectionService = telephonyConnectionService;
        setConferenceHost(conferenceHost);
        if (conferenceHost != null && conferenceHost.getCall() != null
                && conferenceHost.getCall().getPhone() != null) {
            mPhoneAccount = PhoneUtils.makePstnPhoneAccountHandle(
                    conferenceHost.getCall().getPhone());
            Log.v(this, "set phacc to " + mPhoneAccount);
        }

        int imsCapability = Connection.CAPABILITY_VOLTE;
        /// M: WFC  @{
        Phone phone = mConferenceHost.getPhone();
        if (phone != null) {
            Context context = phone.getContext();
            final TelecomManager telecomManager = (TelecomManager) context
                    .getSystemService(context.TELECOM_SERVICE);
            PhoneAccount phoneAccount = telecomManager.getPhoneAccount(mPhoneAccount);
            if (phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_WIFI_CALLING)) {
                imsCapability |= Connection.CAPABILITY_VoWIFI;
            }
        }
        Log.v(this, "[WFC]imsCapability " + imsCapability);
        /// @}
        setConnectionCapabilities(
                Connection.CAPABILITY_SUPPORT_HOLD |
                /// M: ALPS02025770, update it in updateConferenceCapability(). @{
                //Connection.CAPABILITY_HOLD |
                /// @}
                Connection.CAPABILITY_MUTE |
                /// M: for HD icon. @{
                imsCapability
                /// @}
        );

        /// M: For VoLTE enhanced conference call. @{
        if (conferenceHost != null && conferenceHost.getOriginalConnection() != null
                && conferenceHost.getOriginalConnection().getCall().getPhone() != null) {
            if (conferenceHost.getOriginalConnection().getCall().getPhone()
                    .isFeatureSupported(Phone.FeatureType.VOLTE_ENHANCED_CONFERENCE)) {
                addCapability(Connection.CAPABILITY_INVITE_PARTICIPANTS);
            }
        }
        /// @}

        /// M: ALPS02025770. @{
        updateConferenceCapability();
        /// @}
    }

    /**
     * Not used by the IMS conference controller.
     *
     * @return {@code Null}.
     */
    @Override
    public android.telecom.Connection getPrimaryConnection() {
        return null;
    }

    /**
     * Invoked when the Conference and all its {@link Connection}s should be disconnected.
     * <p>
     * Hangs up the call via the conference host connection.  When the host connection has been
     * successfully disconnected, the {@link #mConferenceHostListener} listener receives an
     * {@code onDestroyed} event, which triggers the conference participant connections to be
     * disconnected.
     */
    @Override
    public void onDisconnect() {
        Log.v(this, "onDisconnect: hanging up conference host.");
        mHostCallAddress = null;
        mHostCallState = Connection.STATE_DISCONNECTED;
        if (mConferenceHost == null) {
            return;
        }

        Call call = mConferenceHost.getCall();
        if (call != null) {
            try {
                call.hangup();
            } catch (CallStateException e) {
                Log.e(this, e, "Exception thrown trying to hangup conference");
            }
        }
    }

    /**
     * Invoked when the specified {@link android.telecom.Connection} should be separated from the
     * conference call.
     * <p>
     * IMS does not support separating connections from the conference.
     *
     * @param connection The connection to separate.
     */
    @Override
    public void onSeparate(android.telecom.Connection connection) {
        Log.wtf(this, "Cannot separate connections from an IMS conference.");
    }

    /**
     * Invoked when the specified {@link android.telecom.Connection} should be merged into the
     * conference call.
     *
     * @param connection The {@code Connection} to merge.
     */
    @Override
    public void onMerge(android.telecom.Connection connection) {
        /// M: For VoLTE enhanced conference call. @{
        if (mIsDuringAddingParticipants) {
            toastWhenIsAddingParticipants();
            return;
        }
        /// @}

        try {
            Phone phone = ((TelephonyConnection) connection).getPhone();
            if (phone != null) {
                phone.conference();
            }
        } catch (CallStateException e) {
            Log.e(this, e, "Exception thrown trying to merge call into a conference");
        }
    }

    /**
     * Invoked when the conference should be put on hold.
     */
    @Override
    public void onHold() {
        /// M: For VoLTE enhanced conference call. @{
        if (mIsDuringAddingParticipants) {
            toastWhenIsAddingParticipants();
            return;
        }

        if (mConferenceHost == null) {
            return;
        }
        mConferenceHost.performHold();
    }

    /// M: CC078: For DSDS/DSDA Two-action operation @{
    /**
     * Invoked when the conference should be put on hold, with pending call action, answer?
     * @param pendingCallAction The pending call action.
     */
    @Override
    public void onHold(String pendingCallAction) {
        if (mConferenceHost == null) {
            return;
        }
        mConferenceHost.performHold(pendingCallAction);
    }

    /**
     * Invoked when the Conference and all its {@link Connection}s should be disconnected.
     * with pending call action, answer?
     * @param pendingCallAction The pending call action.
     */
    @Override
    public void onDisconnect(String pendingCallAction) {
        onDisconnect();
    }
    /// @}

    /**
     * Invoked when the conference should be moved from hold to active.
     */
    @Override
    public void onUnhold() {
        /// M: For VoLTE enhanced conference call. @{
        if (mIsDuringAddingParticipants) {
            toastWhenIsAddingParticipants();
            return;
        }
        /// @}

        if (mConferenceHost == null) {
            return;
        }
        mConferenceHost.performUnhold();
    }

    /**
     * Invoked to play a DTMF tone.
     *
     * @param c A DTMF character.
     */
    @Override
    public void onPlayDtmfTone(char c) {
        if (mConferenceHost == null) {
            return;
        }
        mConferenceHost.onPlayDtmfTone(c);
    }

    /**
     * Invoked to stop playing a DTMF tone.
     */
    @Override
    public void onStopDtmfTone() {
        if (mConferenceHost == null) {
            return;
        }
        mConferenceHost.onStopDtmfTone();
    }

    /**
     * Handles the addition of connections to the {@link ImsConference}.  The
     * {@link ImsConferenceController} does not add connections to the conference.
     *
     * @param connection The newly added connection.
     */
    @Override
    public void onConnectionAdded(android.telecom.Connection connection) {
        // No-op
    }

    /**
     * Updates the manage conference capability of the conference.  Where there are one or more
     * conference event package participants, the conference management is permitted.  Where there
     * are no conference event package participants, conference management is not permitted.
     */
    private void updateManageConference() {
        boolean couldManageConference = can(Connection.CAPABILITY_MANAGE_CONFERENCE);
        boolean canManageConference = !mConferenceParticipantConnections.isEmpty();
        Log.v(this, "updateManageConference was:%s is:%s", couldManageConference ? "Y" : "N",
                canManageConference ? "Y" : "N");

        if (couldManageConference != canManageConference) {
            int newCapabilities = getConnectionCapabilities();

            if (canManageConference) {
                addCapability(Connection.CAPABILITY_MANAGE_CONFERENCE);
            } else {
                removeCapability(Connection.CAPABILITY_MANAGE_CONFERENCE);
            }
        }
    }

    /**
     * Sets the connection hosting the conference and registers for callbacks.
     *
     * @param conferenceHost The connection hosting the conference.
     */
    private void setConferenceHost(TelephonyConnection conferenceHost) {
        if (Log.VERBOSE) {
            Log.v(this, "setConferenceHost " + conferenceHost);
        }

        mConferenceHost = conferenceHost;
        mConferenceHost.addConnectionListener(mConferenceHostListener);
        mConferenceHost.addTelephonyConnectionListener(mTelephonyConnectionListener);
    }

    /**
     * Handles state changes for conference participant(s).  The participants data passed in
     *
     * @param parent The connection which was notified of the conference participant.
     * @param participants The conference participant information.
     */
    private void handleConferenceParticipantsUpdate(
            TelephonyConnection parent, List<ConferenceParticipant> participants) {
        if (participants == null) {
            return;
        }
        /// M: ALPS02469251. Some NW only update the status of the
        /// disconnected participants, and will not contain the participants
        /// who's status is no-changed in the XML package.
        /// Workaround to handle this case. @{
        int participantsCount = participants.size();

        /// M: ALPS02487069. The xml might only contained the host state changed,
        /// do early return if the host state is not changed to disconnected. @{
        if (participantsCount == 1 && isHostCallStateChange(participants.get(0))) {
            if (mHostCallState != Connection.STATE_DISCONNECTED) {
                Log.d(this, "Host call state changed = %d", mHostCallState);
                return;
            }
        }
        /// @}

        HashSet<Uri> discParticipantsUserEntities = new HashSet<>(participants.size());
        Iterator<ConferenceParticipant> iterator = participants.iterator();
        synchronized (this) {
            while (iterator.hasNext()) {
                ConferenceParticipant participant = iterator.next();
                if (participant.getState() == Connection.STATE_DISCONNECTED) {
                    Uri userEntity = participant.getHandle();
                    discParticipantsUserEntities.add(userEntity);
                    // remove disconnected participants from original participants list
                    iterator.remove();
                }
            }
        }
        Log.d(this, "Participants with disconnected status: %d, all updated participants: %d",
               discParticipantsUserEntities.size(), participantsCount);
        if (discParticipantsUserEntities.size() == participantsCount) {
            // all modified participants status is "disconnected"
            removeModifyToDisconnectParticipants(discParticipantsUserEntities);
            return;
        }
        /// @}

        /// M: ALPS02469251. Try to get the host address via getLine1Number(). @{
        if (mConferenceHost != null && mConferenceHost.getPhone() != null) {
            Context ctx = mConferenceHost.getPhone().getContext();
            TelephonyManager telMngr = (TelephonyManager)
                    ctx.getSystemService(Context.TELEPHONY_SERVICE);
            String hostConnNum = telMngr.getLine1Number();
            if (hostConnNum != null) {
                Log.d(this, "hostConnNum from teleMngr: " + hostConnNum);
                Iterator<ConferenceParticipant> iter = participants.iterator();
                while (iter.hasNext()) {
                    ConferenceParticipant participant = iter.next();
                    Uri userEntity = participant.getHandle();
                    Log.w(this, "callAddr from xml: " + userEntity);
                    if (PhoneNumberUtils.compareLoosely(
                            userEntity.toString(), hostConnNum)) {
                        Log.d(this, "Remove host: %s from participants.", userEntity.toString());
                        mHostCallAddress = userEntity;
                        mHostCallState = participant.getState();
                        break;
                    }
                }
            }
        }
        /// @}

        boolean newParticipantsAdded = false;
        boolean oldParticipantsRemoved = false;
        ArrayList<ConferenceParticipant> newParticipants = new ArrayList<>(participants.size());
        /// M: use user entity instead of endpoints. @{
        //HashSet<Uri> participantEndpoints = new HashSet<>(participants.size());
        HashSet<Uri> participantEntities = new HashSet<>(participants.size());
        /// @}
        /// M: existing participants state update counter
        int stateUpdateCounter = 0;

        // Add any new participants and update existing.
        for (ConferenceParticipant participant : participants) {
            Uri entity = participant.getHandle();
            /// M: if host call addr is null, 
            /// consider the first one in the first xml as the host connection. @{
            if (mHostCallAddress == null) {
                Log.w(this, "The first one is the host connection.");
                mHostCallAddress = entity;
                mHostCallState = participant.getState();
                continue;
            }

            /// M: ALPS02469251. The order of participants might be changed.
            /// Skip the participant whose, user entity is the same as the host. @{
            if (mHostCallAddress != null && mHostCallAddress.equals(entity)) {
                Log.d(this, "Skip the participant, equals to host");
                mHostCallState = participant.getState();
                continue;
            }
            /// @}

            //Uri endpoint = participant.getEndpoint();
            participantEntities.add(entity);
            if (!mConferenceParticipantConnections.containsKey(entity)) {
                /// M: ALPS02058672. Filter out the disconnected participants. @{
                /// If a participant stay in the XML file with disconnected state, it will cause
                /// createConferenceParticipantConnection() is called whenever XML is received.
                if (participant.getState() == Connection.STATE_DISCONNECTED) {
                    Log.w(this, "ignore for disconnected participant:" + participant);
                    continue;
                }
                /// @}
                Log.w(this, "add new participant: " + entity);
                createConferenceParticipantConnection(parent, participant);
                newParticipants.add(participant);
                newParticipantsAdded = true;
            } else {
                Log.w(this, "update existing participant: " + entity);
                ConferenceParticipantConnection connection =
                        mConferenceParticipantConnections.get(entity);
                connection.updateState(participant.getState());
                ++stateUpdateCounter;
            }
            /// @}
        }
        /// M: ALPS02487069. All participants in xml is for state update. @{
        if (participantsCount == stateUpdateCounter) {
            Log.d(this, "The xml is an updating xml");
            return;
        }
        /// @}

        // Set state of new participants.
        if (newParticipantsAdded) {
            // Set the state of the new participants at once and add to the conference
            for (ConferenceParticipant newParticipant : newParticipants) {
                ConferenceParticipantConnection connection =
                        /// M: @{
                        //mConferenceParticipantConnections.get(newParticipant.getEndpoint());
                        mConferenceParticipantConnections.get(newParticipant.getHandle());
                        /// @}
                connection.updateState(newParticipant.getState());
            }
            /// M: ALPS02469251. If all participants is adding, update and return.
            if (participantsCount == newParticipants.size()) {
                updateManageConference();
                return;
            }
        }

        // Finally, remove any participants from the conference that no longer exist in the
        // conference event package data.
        Iterator<Map.Entry<Uri, ConferenceParticipantConnection>> entryIterator =
                mConferenceParticipantConnections.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Uri, ConferenceParticipantConnection> entry = entryIterator.next();

            /// M: @{
            //if (!participantEndpoints.contains(entry.getKey())) {
            if (!participantEntities.contains(entry.getKey())) {
                Log.w(this, "remove existing participant: " + entry.getKey());
                /// @}
                ConferenceParticipantConnection participant = entry.getValue();
                /// M: ALPS02059441. Disconnect before removing it. @{
                if (participant.getState() != Connection.STATE_DISCONNECTED) {
                    participant.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
                }
                /// @}
                participant.removeConnectionListener(mParticipantListener);
                removeConnection(participant);
                entryIterator.remove();
                oldParticipantsRemoved = true;
            }
        }

        // If new participants were added or old ones were removed, we need to ensure the state of
        // the manage conference capability is updated.
        if (newParticipantsAdded || oldParticipantsRemoved) {
            updateManageConference();
        }
    }

    /**
     * Creates a new {@link ConferenceParticipantConnection} to represent a
     * {@link ConferenceParticipant}.
     * <p>
     * The new connection is added to the conference controller and connection service.
     *
     * @param parent The connection which was notified of the participant change (e.g. the
     *                         parent connection).
     * @param participant The conference participant information.
     */
    private void createConferenceParticipantConnection(
            TelephonyConnection parent, ConferenceParticipant participant) {

        // Create and add the new connection in holding state so that it does not become the
        // active call.
        ConferenceParticipantConnection connection = new ConferenceParticipantConnection(
                parent.getOriginalConnection(), participant);
        connection.addConnectionListener(mParticipantListener);

        if (Log.VERBOSE) {
            Log.v(this, "createConferenceParticipantConnection: %s", connection);
        }

        /// M: @{
        //mConferenceParticipantConnections.put(participant.getEndpoint(), connection);
        mConferenceParticipantConnections.put(participant.getHandle(), connection);
        /// @}
        PhoneAccountHandle phoneAccountHandle =
                PhoneUtils.makePstnPhoneAccountHandle(parent.getPhone());
        mTelephonyConnectionService.addExistingConnection(phoneAccountHandle, connection);
        addConnection(connection);
    }

    /**
     * Removes a conference participant from the conference.
     *
     * @param participant The participant to remove.
     */
    private void removeConferenceParticipant(ConferenceParticipantConnection participant) {
        if (Log.VERBOSE) {
            Log.v(this, "removeConferenceParticipant: %s", participant);
        }

        participant.removeConnectionListener(mParticipantListener);
        participant.getEndpoint();
        /// M: @{
        //mConferenceParticipantConnections.remove(participant.getEndpoint());
        mConferenceParticipantConnections.remove(participant.getAddress());
        /// @}
    }

    /**
     * Disconnects all conference participants from the conference.
     */
    private void disconnectConferenceParticipants() {
        Log.v(this, "disconnectConferenceParticipants");

        for (ConferenceParticipantConnection connection :
                mConferenceParticipantConnections.values()) {

            removeConferenceParticipant(connection);

            // Mark disconnect cause as cancelled to ensure that the call is not logged in the
            // call log.
            connection.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
            connection.destroy();
        }
        mConferenceParticipantConnections.clear();
    }

    /**
     * Handles a change in the original connection backing the conference host connection.  This can
     * happen if an SRVCC event occurs on the original IMS connection, requiring a fallback to
     * GSM or CDMA.
     * <p>
     * If this happens, we will add the conference host connection to telecom and tear down the
     * conference.
     */
    private void handleOriginalConnectionChange() {
        if (mConferenceHost == null) {
            Log.w(this, "handleOriginalConnectionChange; conference host missing.");
            return;
        }

        com.android.internal.telephony.Connection originalConnection =
                mConferenceHost.getOriginalConnection();

        if (!(originalConnection instanceof ImsPhoneConnection)) {
            if (Log.VERBOSE) {
                Log.v(this,
                        "Original connection for conference host is no longer an IMS connection; " +
                                "new connection: %s", originalConnection);
            }

            PhoneAccountHandle phoneAccountHandle =
                    PhoneUtils.makePstnPhoneAccountHandle(mConferenceHost.getPhone());
            mTelephonyConnectionService.addExistingConnection(phoneAccountHandle, mConferenceHost);
            mConferenceHost.removeConnectionListener(mConferenceHostListener);
            mConferenceHost.removeTelephonyConnectionListener(mTelephonyConnectionListener);
            mConferenceHost = null;
            setDisconnected(new DisconnectCause(DisconnectCause.OTHER));
            ///M:[ALPS02023641]Certainly remove the participant connections @{
            disconnectConferenceParticipants();
            /// @}
            destroy();
        }
    }

    /**
     * Changes the state of the Ims conference.
     *
     * @param state the new state.
     */
    public void setState(int state) {
        Log.v(this, "setState %s", Connection.stateToString(state));

        switch (state) {
            case Connection.STATE_INITIALIZING:
            case Connection.STATE_NEW:
            //case Connection.STATE_RINGING:
            //case Connection.STATE_DIALING:
                // No-op -- not applicable.
                break;
            /// M: For enhanced conference. @{
            case Connection.STATE_RINGING:
                setRinging();
                break;
            case Connection.STATE_DIALING:
                setDialing();
                break;
            /// @}
            case Connection.STATE_DISCONNECTED:
                DisconnectCause disconnectCause;
                if (mConferenceHost == null) {
                    disconnectCause = new DisconnectCause(DisconnectCause.CANCELED);
                } else {
                    disconnectCause = DisconnectCauseUtil.toTelecomDisconnectCause(
                            mConferenceHost.getOriginalConnection().getDisconnectCause());
                }
                setDisconnected(disconnectCause);
                destroy();
                break;
            case Connection.STATE_ACTIVE:
                setActive();
                break;
            case Connection.STATE_HOLDING:
                setOnHold();
                break;
        }

        /// M: ALPS02025770. @{
        updateConferenceCapability();
        /// @}
    }

    /**
     * Builds a string representation of the {@link ImsConference}.
     *
     * @return String representing the conference.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ImsConference objId:");
        sb.append(System.identityHashCode(this));
        sb.append(" state:");
        sb.append(Connection.stateToString(getState()));
        sb.append("capability:");
        sb.append(Connection.capabilitiesToString(getConnectionCapabilities()));
        sb.append(" hostConnection:");
        sb.append(mConferenceHost);
        sb.append(" participants:");
        sb.append(mConferenceParticipantConnections.size());
        sb.append("]");
        return sb.toString();
    }

    /// M: @{
    int getNumbOfParticipants() {
        return mConferenceParticipantConnections.size();
    }

    Phone getPhone() {
        if (mConferenceHost == null) {
            return null;
        }
        return mConferenceHost.getPhone();
    }
    /// @}

    /// M: For VoLTE enhanced conference call. @{
    @Override
    public void onInviteConferenceParticipants(List<String> numbers) {
        if (mConferenceHost == null) {
            return;
        }

        // Judge whether the invited number has already existed in the conference
        Iterator<String> iter = numbers.iterator();
        while (iter.hasNext()) {
            if (hasExistedInConference(iter.next())) {
                iter.remove();
            }
        }
        if (numbers.size() == 0) {
            return;
        }

        ///  M: ALPS02209724. Show toast if the conference is full. @{
        if (getNumbOfParticipants() + numbers.size() > IMS_CONFERENCE_MAX_SIZE) {
            if (mConferenceHost.getPhone() != null) {
                toastWhenConferenceIsFull(mConferenceHost.getPhone().getContext());
            }
        }
        /// @}

        mConferenceHost.performInviteConferenceParticipants(numbers);
        mIsDuringAddingParticipants = true;
    }

    private boolean hasExistedInConference(String number) {
        for (Map.Entry<Uri, ConferenceParticipantConnection> entry :
                mConferenceParticipantConnections.entrySet()) {
            String participantNumber = entry.getKey().getSchemeSpecificPart();
            Log.w(this, "The invited number is %s and participant number is %s",
                    number, participantNumber);
            if (PhoneNumberUtils.compare(number, participantNumber)) {
                Log.v(this, "The invited number has already existed in the conference");
                return true;
            }
        }

        if (mHostCallAddress != null && mHostCallAddress.getSchemeSpecificPart() != null) {
            if (PhoneNumberUtils.compare(number, mHostCallAddress.getSchemeSpecificPart())) {
                Log.v(this, "The invited number is the host connection address.");
                return true;
            }
        }

        return false;
    }

    /**
     * Popup toast when user performs hold/unhold conference if adding
     * participants has not been yet completed.
     */
    private void toastWhenIsAddingParticipants() {
        if (mConferenceHost == null) {
            return;
        }

        Context context;
        Phone phone = mConferenceHost.getPhone();
        if (phone != null) {
            context = phone.getContext();
            Toast.makeText(context,
                    context.getString(R.string.volte_is_adding_participants), Toast.LENGTH_SHORT).
                        show();
        }
    }
    /// @}

    /// M: For conference SRVCC. @{
    private void handleConferenceSRVCC(
            ArrayList<com.android.internal.telephony.Connection> radioConnections) {
        Log.w(this, "handleConferenceSRVCC");

        if (mConferenceHost == null) {
            Log.w(this, "onConferenceConnectionsConfigured: conference host missing.");
            return;
        }

        if (radioConnections == null || radioConnections.size() < 2) {
            Log.w(this, "onConferenceConnectionsConfigured: failed at radioConnections.");
            return;
        }

        disconnectConferenceParticipants();
        mTelephonyConnectionService.performImsConferenceSRVCC(this, radioConnections);

        mConferenceHost.removeConnectionListener(mConferenceHostListener);
        mConferenceHost.removeTelephonyConnectionListener(mTelephonyConnectionListener);
        mConferenceHost = null;
        destroy();
    }
    /// @}

    /// M: ALPS02008029. @{
    /**
     * Invoked when the conference call should be swap with background call.
     * @hide
     */
    @Override
    public final void onSwapWithBackgroundCall() {
        if (mConferenceHost == null) {
            return;
        }
        mConferenceHost.onSwapWithBackgroundCall();
    }
    /// @}

    /// M: ALPS02025770. @{
    void updateConferenceCapability() {
        if (mTelephonyConnectionService != null) {
            if (mTelephonyConnectionService.canHold(this)) {
                addCapability(Connection.CAPABILITY_HOLD);
            } else {
                removeCapability(Connection.CAPABILITY_HOLD);
            }

            if (mTelephonyConnectionService.canUnHold(this)) {
                addCapability(Connection.CAPABILITY_UNHOLD);
            } else {
                removeCapability(Connection.CAPABILITY_UNHOLD);
            }
        }
    }
    /// @}


    /// M: ALPS02209724. Popup toast when conference reach the maximum participants. @{
    /**
     * For below cases, PhoneApp popups the toast to hint user the conference is full.
     * case-1: when merging the 6th call into a conference.
     * case-2: when inviting participant to cause conference has more than 6 participants.
     * case-3: when dialing a conference with over 6 participants.
     *
     * @param context the context used to show toast.
     * @hide
     */
    static void toastWhenConferenceIsFull(Context context) {
        if (context == null) {
            return;
        }
        Toast.makeText(context,
                context.getString(R.string.volte_conf_member_reach_max), Toast.LENGTH_SHORT)
                .show();
    }
    /// @}
    /// M: ALPS02469251. Some operators only update the status of the
    /// disconnected participants, and will not contain the participants
    /// who's status is no-changed in the XML package.
    /// Workaround to handle this case. @{
    /**
     * Remove participants whose are updated to disconnect.
     *
     * @param discParticipantsUserEntities, participants user entity uri
     * @hide
     */
    private void removeModifyToDisconnectParticipants(HashSet<Uri> discParticipantsUserEntities) {
        Log.d(this, "remove modify to disconnect participants: " +
                                       discParticipantsUserEntities.toString());

        Iterator<Map.Entry<Uri, ConferenceParticipantConnection>> entryIterator =
                            mConferenceParticipantConnections.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Uri, ConferenceParticipantConnection> entry = entryIterator.next();
            if (discParticipantsUserEntities.contains(entry.getKey())) {
                Log.d(this, "remove existing participant: " + entry.getKey());
                ConferenceParticipantConnection participant = entry.getValue();
                removeExistingParticipantConnection(participant);
                entryIterator.remove();
            }
        }
        updateManageConference();
    }

    /**
     * Remove existing participant connection and unregister the listener.
     *
     * @param participant, disconnected existing participant
     * @hide
     */
    private void removeExistingParticipantConnection(ConferenceParticipantConnection participant) {
        participant.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
        participant.removeConnectionListener(mParticipantListener);
        mTelephonyConnectionService.removeConnection(participant);
        removeConnection(participant);
    }
    /// @}
    /// M: ALPS02487069. If the participant is host, update the host state @{
    /**
     * Remove existing participant connection and unregister the listener.
     *
     * @param participant, disconnected existing participant
     * @hide
     */
    private boolean isHostCallStateChange(ConferenceParticipant participant) {
        Uri userEntity = participant.getHandle();
        boolean ret = false;
        if (mHostCallAddress != null && mHostCallAddress.equals(userEntity)) {
            int state = participant.getState();
            if (mHostCallState != state) {
                mHostCallState = state;
                ret = true;
            }
        }
        return ret;
    }
    /// @}
}
