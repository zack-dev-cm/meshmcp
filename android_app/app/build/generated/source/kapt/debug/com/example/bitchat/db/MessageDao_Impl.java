package com.example.bitchat.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MessageDao_Impl implements MessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity;

  private final EntityDeletionOrUpdateAdapter<MessageEntity> __updateAdapterOfMessageEntity;

  private final SharedSQLiteStatement __preparedStmtOfWipe;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessageEntity = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `messages` (`id`,`sender`,`content`,`timestamp`,`isRelay`,`originalSender`,`isPrivate`,`recipientNickname`,`senderPeerId`,`deliveryStatus`,`retryCount`,`isFavorite`,`delivered`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getSender() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getSender());
        }
        if (entity.getContent() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getContent());
        }
        statement.bindLong(4, entity.getTimestamp());
        final int _tmp = entity.isRelay() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getOriginalSender() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getOriginalSender());
        }
        final int _tmp_1 = entity.isPrivate() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getRecipientNickname() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getRecipientNickname());
        }
        if (entity.getSenderPeerId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getSenderPeerId());
        }
        if (entity.getDeliveryStatus() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getDeliveryStatus());
        }
        statement.bindLong(11, entity.getRetryCount());
        final int _tmp_2 = entity.isFavorite() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
        final int _tmp_3 = entity.getDelivered() ? 1 : 0;
        statement.bindLong(13, _tmp_3);
      }
    };
    this.__updateAdapterOfMessageEntity = new EntityDeletionOrUpdateAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `messages` SET `id` = ?,`sender` = ?,`content` = ?,`timestamp` = ?,`isRelay` = ?,`originalSender` = ?,`isPrivate` = ?,`recipientNickname` = ?,`senderPeerId` = ?,`deliveryStatus` = ?,`retryCount` = ?,`isFavorite` = ?,`delivered` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getSender() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getSender());
        }
        if (entity.getContent() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getContent());
        }
        statement.bindLong(4, entity.getTimestamp());
        final int _tmp = entity.isRelay() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getOriginalSender() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getOriginalSender());
        }
        final int _tmp_1 = entity.isPrivate() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getRecipientNickname() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getRecipientNickname());
        }
        if (entity.getSenderPeerId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getSenderPeerId());
        }
        if (entity.getDeliveryStatus() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getDeliveryStatus());
        }
        statement.bindLong(11, entity.getRetryCount());
        final int _tmp_2 = entity.isFavorite() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
        final int _tmp_3 = entity.getDelivered() ? 1 : 0;
        statement.bindLong(13, _tmp_3);
        if (entity.getId() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getId());
        }
      }
    };
    this.__preparedStmtOfWipe = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MessageEntity message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessageEntity.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final MessageEntity message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMessageEntity.handle(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object wipe(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfWipe.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfWipe.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MessageEntity>> getAll() {
    final String _sql = "SELECT * FROM messages ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsRelay = CursorUtil.getColumnIndexOrThrow(_cursor, "isRelay");
          final int _cursorIndexOfOriginalSender = CursorUtil.getColumnIndexOrThrow(_cursor, "originalSender");
          final int _cursorIndexOfIsPrivate = CursorUtil.getColumnIndexOrThrow(_cursor, "isPrivate");
          final int _cursorIndexOfRecipientNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientNickname");
          final int _cursorIndexOfSenderPeerId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderPeerId");
          final int _cursorIndexOfDeliveryStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveryStatus");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "delivered");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpSender;
            if (_cursor.isNull(_cursorIndexOfSender)) {
              _tmpSender = null;
            } else {
              _tmpSender = _cursor.getString(_cursorIndexOfSender);
            }
            final String _tmpContent;
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null;
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsRelay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRelay);
            _tmpIsRelay = _tmp != 0;
            final String _tmpOriginalSender;
            if (_cursor.isNull(_cursorIndexOfOriginalSender)) {
              _tmpOriginalSender = null;
            } else {
              _tmpOriginalSender = _cursor.getString(_cursorIndexOfOriginalSender);
            }
            final boolean _tmpIsPrivate;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPrivate);
            _tmpIsPrivate = _tmp_1 != 0;
            final String _tmpRecipientNickname;
            if (_cursor.isNull(_cursorIndexOfRecipientNickname)) {
              _tmpRecipientNickname = null;
            } else {
              _tmpRecipientNickname = _cursor.getString(_cursorIndexOfRecipientNickname);
            }
            final String _tmpSenderPeerId;
            if (_cursor.isNull(_cursorIndexOfSenderPeerId)) {
              _tmpSenderPeerId = null;
            } else {
              _tmpSenderPeerId = _cursor.getString(_cursorIndexOfSenderPeerId);
            }
            final String _tmpDeliveryStatus;
            if (_cursor.isNull(_cursorIndexOfDeliveryStatus)) {
              _tmpDeliveryStatus = null;
            } else {
              _tmpDeliveryStatus = _cursor.getString(_cursorIndexOfDeliveryStatus);
            }
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final boolean _tmpIsFavorite;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_2 != 0;
            final boolean _tmpDelivered;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDelivered);
            _tmpDelivered = _tmp_3 != 0;
            _item = new MessageEntity(_tmpId,_tmpSender,_tmpContent,_tmpTimestamp,_tmpIsRelay,_tmpOriginalSender,_tmpIsPrivate,_tmpRecipientNickname,_tmpSenderPeerId,_tmpDeliveryStatus,_tmpRetryCount,_tmpIsFavorite,_tmpDelivered);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object undeliveredForPeer(final String peerId,
      final Continuation<? super List<MessageEntity>> $completion) {
    final String _sql = "SELECT * FROM messages WHERE delivered = 0 AND senderPeerId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (peerId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, peerId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsRelay = CursorUtil.getColumnIndexOrThrow(_cursor, "isRelay");
          final int _cursorIndexOfOriginalSender = CursorUtil.getColumnIndexOrThrow(_cursor, "originalSender");
          final int _cursorIndexOfIsPrivate = CursorUtil.getColumnIndexOrThrow(_cursor, "isPrivate");
          final int _cursorIndexOfRecipientNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientNickname");
          final int _cursorIndexOfSenderPeerId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderPeerId");
          final int _cursorIndexOfDeliveryStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveryStatus");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfDelivered = CursorUtil.getColumnIndexOrThrow(_cursor, "delivered");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpSender;
            if (_cursor.isNull(_cursorIndexOfSender)) {
              _tmpSender = null;
            } else {
              _tmpSender = _cursor.getString(_cursorIndexOfSender);
            }
            final String _tmpContent;
            if (_cursor.isNull(_cursorIndexOfContent)) {
              _tmpContent = null;
            } else {
              _tmpContent = _cursor.getString(_cursorIndexOfContent);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsRelay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRelay);
            _tmpIsRelay = _tmp != 0;
            final String _tmpOriginalSender;
            if (_cursor.isNull(_cursorIndexOfOriginalSender)) {
              _tmpOriginalSender = null;
            } else {
              _tmpOriginalSender = _cursor.getString(_cursorIndexOfOriginalSender);
            }
            final boolean _tmpIsPrivate;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPrivate);
            _tmpIsPrivate = _tmp_1 != 0;
            final String _tmpRecipientNickname;
            if (_cursor.isNull(_cursorIndexOfRecipientNickname)) {
              _tmpRecipientNickname = null;
            } else {
              _tmpRecipientNickname = _cursor.getString(_cursorIndexOfRecipientNickname);
            }
            final String _tmpSenderPeerId;
            if (_cursor.isNull(_cursorIndexOfSenderPeerId)) {
              _tmpSenderPeerId = null;
            } else {
              _tmpSenderPeerId = _cursor.getString(_cursorIndexOfSenderPeerId);
            }
            final String _tmpDeliveryStatus;
            if (_cursor.isNull(_cursorIndexOfDeliveryStatus)) {
              _tmpDeliveryStatus = null;
            } else {
              _tmpDeliveryStatus = _cursor.getString(_cursorIndexOfDeliveryStatus);
            }
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final boolean _tmpIsFavorite;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_2 != 0;
            final boolean _tmpDelivered;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfDelivered);
            _tmpDelivered = _tmp_3 != 0;
            _item = new MessageEntity(_tmpId,_tmpSender,_tmpContent,_tmpTimestamp,_tmpIsRelay,_tmpOriginalSender,_tmpIsPrivate,_tmpRecipientNickname,_tmpSenderPeerId,_tmpDeliveryStatus,_tmpRetryCount,_tmpIsFavorite,_tmpDelivered);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
