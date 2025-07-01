package it.gov.pagopa.pu.sil.util;

import jakarta.activation.DataSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ByteArrayDataSource implements DataSource {

  private final byte[] data;

  private final String contentType;

  public ByteArrayDataSource(String contentType, byte[] data) {
    this.contentType = contentType;
    this.data = data;
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(this.data);
  }

  @Override
  public OutputStream getOutputStream() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getContentType() {
    return this.contentType;
  }

  @Override
  public String getName() {
    return "ByteArrayDataSource";
  }
}

