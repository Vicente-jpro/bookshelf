package com.example.bookshelf.service;

import com.example.bookshelf.dto.BookDTO;
import com.example.bookshelf.entity.Book;
import com.example.bookshelf.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public BookDTO createBook(String title, String description, BigDecimal price, MultipartFile image) throws IOException {
        Book book = new Book();
        book.setTitle(title);
        book.setDescription(description);
        book.setPrice(price);

        if (image != null && !image.isEmpty()) {
            String imagePath = saveImage(image);
            book.setImagePath(imagePath);
        }

        Book savedBook = bookRepository.save(book);
        return convertToDTO(savedBook);
    }

    public BookDTO updateBook(Long id, String title, String description, BigDecimal price, MultipartFile image) throws IOException {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        book.setTitle(title);
        book.setDescription(description);
        book.setPrice(price);

        if (image != null && !image.isEmpty()) {
            // Delete old image if exists
            if (book.getImagePath() != null) {
                deleteImage(book.getImagePath());
            }
            String imagePath = saveImage(image);
            book.setImagePath(imagePath);
        }

        Book updatedBook = bookRepository.save(book);
        return convertToDTO(updatedBook);
    }

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BookDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        return convertToDTO(book);
    }

    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        
        if (book.getImagePath() != null) {
            deleteImage(book.getImagePath());
        }
        
        bookRepository.deleteById(id);
    }

    private String saveImage(MultipartFile file) throws IOException {
        // Validate file
        validateImageFile(file);
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase() 
                : ".jpg";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFilename;
    }

    private void validateImageFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        // Check file size (10MB max)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IOException("File size exceeds maximum limit of 10MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Only image files are allowed");
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.matches("jpg|jpeg|png|gif|bmp|webp")) {
                throw new IOException("Invalid image file extension. Allowed: jpg, jpeg, png, gif, bmp, webp");
            }
        }
    }

    private void deleteImage(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw exception
            System.err.println("Failed to delete image: " + filename);
        }
    }

    private BookDTO convertToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setDescription(book.getDescription());
        dto.setPrice(book.getPrice());
        dto.setImagePath(book.getImagePath());
        
        // Convert imagePath to full imageUrl
        if (book.getImagePath() != null && !book.getImagePath().isEmpty()) {
            dto.setImageUrl("/api/books/images/" + book.getImagePath());
        }
        
        return dto;
    }
}
