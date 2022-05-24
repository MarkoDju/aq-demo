package com.example;

import oracle.sql.CHAR;
import oracle.sql.Datum;
import oracle.sql.NUMBER;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

import java.sql.Connection;
import java.sql.SQLException;

public class AqMessage implements ORAData, ORADataFactory {
  oracle.sql.NUMBER ident;
  oracle.sql.CHAR message;

  static final AqMessage _personFactory = new AqMessage();

  public static ORADataFactory getORADataFactory()
  {
    return _personFactory;
  }

  public AqMessage () {}

  public AqMessage(NUMBER ident, CHAR message)
  {
    this.ident = ident;
    this.message = message;
  }

  @Override
  public Datum toDatum(Connection connection) throws SQLException {
    StructDescriptor sd =
            StructDescriptor.createDescriptor("SYSTEM.MESSAGE_T", connection);
    Object [] attributes = { ident, message };
    return new STRUCT(sd, connection, attributes);
  }

  @Override
  public ORAData create(Datum datum, int i) throws SQLException {
    if (datum == null) {
      return null;
    }
    Object [] attributes = ((STRUCT) datum).getOracleAttributes();
    return new AqMessage((NUMBER) attributes[0],
            (CHAR) attributes[1]);
  }
}
