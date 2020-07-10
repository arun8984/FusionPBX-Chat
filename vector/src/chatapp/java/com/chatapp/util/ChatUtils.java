package com.chatapp.util;

import android.content.Context;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.model.RoomMember;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ChatUtils {
    private static final int ROOM_SIZE_ONE_TO_ONE = 2;
    /**
     * Return all the 1:1 rooms joined by the searched user and by the current logged in user.
     * This method go through all the rooms, and for each room, tests if the searched user
     * and the logged in user are present.
     *
     * @param aSession        session
     * @param aSearchedUserId the searched user ID
     * @return an array containing the found rooms
     */
    // Commented out as unused

    public static List<Room> findOneToOneRoomList(final MXSession aSession, final String aSearchedUserId) {
        List<Room> listRetValue = new ArrayList<>();
        List<RoomMember> roomMembersList;
        String userId0, userId1;

        if ((null != aSession) && (null != aSearchedUserId)) {
            Collection<Room> roomsList = aSession.getDataHandler().getStore().getRooms();

            for (Room room : roomsList) {

                //roomMembersList = (List<RoomMember>) room.getMember ();

                if ((null != room.getMember(aSearchedUserId)) && (ROOM_SIZE_ONE_TO_ONE == room.getNumberOfMembers())) {
                    listRetValue.add(room);

                }
            }
        }

        return listRetValue;
    }

    /**
     * Display a toast
     *
     * @param aContext       the context.
     * @param aTextToDisplay the text to display.
     */
    public static void displayToast(Context aContext, CharSequence aTextToDisplay) {
        Toast.makeText(aContext, aTextToDisplay, Toast.LENGTH_SHORT).show();
    }

}
