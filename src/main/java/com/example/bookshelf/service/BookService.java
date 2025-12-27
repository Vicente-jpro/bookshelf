package com.example.bookshelf.service;

import com.example.bookshelf.dto.BookDTO;
import com.example.bookshelf.entity.Book;
import com.example.bookshelf.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CloudStorageService cloudStorageService;

    public BookDTO createBook(String title, String description, BigDecimal price, MultipartFile image) throws IOException {
        Book book = new Book();
        book.setTitle(title);
        book.setDescription(description);
        book.setPrice(price);

        if (image != null && !image.isEmpty()) {
            String imageUrl = cloudStorageService.uploadImage(image);
            book.setImagePath(imageUrl);
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
                cloudStorageService.deleteImage(book.getImagePath());
            }
            String imageUrl = cloudStorageService.uploadImage(image);
            book.setImagePath(imageUrl);
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
            cloudStorageService.deleteImage(book.getImagePath());
        }
        
        bookRepository.deleteById(id);
    }

    private BookDTO convertToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setDescription(book.getDescription());
        dto.setPrice(book.getPrice());
        
        // Set both imagePath (GCS URL) and imageUrl (same for cloud storage)
        if (book.getImagePath() != null && !book.getImagePath().isEmpty()) {
            dto.setImagePath(book.getImagePath());
            dto.setImageUrl(book.getImagePath());
        }
        
        return dto;
    }
}
