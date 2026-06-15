
package com.pfe.docextraction.entity;

import com.pfe.docextraction.entity.base.BaseEntity;
import com.pfe.docextraction.enums.DocumentStatus;
import com.pfe.docextraction.enums.DocumentType;
import com.pfe.docextraction.enums.FileType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "documents", indexes = {
    
    @Index(name = "idx_documents_uploaded_by", columnList = "uploaded_by"),
   
    @Index(name = "idx_documents_status", columnList = "status"),
    
    @Index(name = "idx_documents_deleted_at", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    
    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

   
    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

  
    @Column(name = "file_path", nullable = false)
    private String filePath;


    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

 
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

 
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

  
    @Column(name = "checksum", length = 64)
    private String checksum;

 

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    @Builder.Default
    private DocumentType documentType = DocumentType.AUTRE;

   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

  
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Extraction> extractions;

 

   
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}
