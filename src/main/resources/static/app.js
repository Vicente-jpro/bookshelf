// API Configuration
const API_BASE_URL = '/api/books';

// DOM Elements
const bookForm = document.getElementById('book-form');
const titleInput = document.getElementById('title');
const descriptionInput = document.getElementById('description');
const priceInput = document.getElementById('price');
const imageInput = document.getElementById('image');
const bookIdInput = document.getElementById('book-id');
const booksGrid = document.getElementById('books-grid');
const loadingDiv = document.getElementById('loading');
const errorMessageDiv = document.getElementById('error-message');
const formTitle = document.getElementById('form-title');
const submitText = document.getElementById('submit-text');
const cancelBtn = document.getElementById('cancel-btn');
const modal = document.getElementById('book-modal');
const modalBody = document.getElementById('modal-body');
const closeModal = document.querySelector('.close');

// State
let isEditMode = false;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadBooks();
    setupEventListeners();
});

// Event Listeners
function setupEventListeners() {
    bookForm.addEventListener('submit', handleFormSubmit);
    cancelBtn.addEventListener('click', resetForm);
    closeModal.addEventListener('click', () => modal.style.display = 'none');
    window.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });
}

// Load all books
async function loadBooks() {
    try {
        loadingDiv.style.display = 'block';
        errorMessageDiv.style.display = 'none';
        booksGrid.innerHTML = '';

        const response = await fetch(API_BASE_URL);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const books = await response.json();
        loadingDiv.style.display = 'none';

        if (books.length === 0) {
            booksGrid.innerHTML = `
                <div class="empty-state" style="grid-column: 1/-1;">
                    <div class="icon">📚</div>
                    <h3>No books yet</h3>
                    <p>Add your first book to get started!</p>
                </div>
            `;
        } else {
            books.forEach(book => displayBook(book));
        }
    } catch (error) {
        console.error('Error loading books:', error);
        loadingDiv.style.display = 'none';
        showError('Failed to load books. Please make sure the server is running on port 8080.');
    }
}

// Display a book card
function displayBook(book) {
    const bookCard = document.createElement('div');
    bookCard.className = 'book-card';
    
    const imageUrl = book.imageUrl ? book.imageUrl : null;
    const imageHtml = imageUrl 
        ? `<img src="${imageUrl}" alt="${book.title}">`
        : `<div style="width: 100%; height: 200px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); display: flex; align-items: center; justify-content: center; color: white; font-size: 4rem;">📖</div>`;

    bookCard.innerHTML = `
        <div class="book-image">
            ${imageHtml}
        </div>
        <div class="book-info">
            <h3 class="book-title">${escapeHtml(book.title)}</h3>
            <p class="book-description">${escapeHtml(book.description)}</p>
            <div class="book-price">$${book.price.toFixed(2)}</div>
            <div class="book-actions">
                <button class="btn btn-edit" onclick="editBook(${book.id})" title="Edit book">
                    ✏️ Edit
                </button>
                <button class="btn btn-danger" onclick="deleteBook(${book.id})" title="Delete book">
                    🗑️ Delete
                </button>
            </div>
        </div>
    `;

    // Add click event to view details (except on buttons)
    bookCard.addEventListener('click', (e) => {
        if (!e.target.closest('button')) {
            viewBookDetails(book);
        }
    });

    booksGrid.appendChild(bookCard);
}

// View book details in modal
function viewBookDetails(book) {
    const imageHtml = book.imageUrl 
        ? `<img src="${book.imageUrl}" alt="${book.title}" class="modal-image">`
        : `<div style="width: 100%; height: 300px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); display: flex; align-items: center; justify-content: center; color: white; font-size: 6rem; border-radius: 10px; margin-bottom: 20px;">📖</div>`;

    modalBody.innerHTML = `
        ${imageHtml}
        <h2 style="color: var(--primary-color); margin-bottom: 15px;">${escapeHtml(book.title)}</h2>
        <p style="color: var(--secondary-color); line-height: 1.6; margin-bottom: 20px;">${escapeHtml(book.description)}</p>
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <div class="book-price" style="margin: 0;">$${book.price.toFixed(2)}</div>
            <div style="display: flex; gap: 10px;">
                <button class="btn btn-edit" onclick="editBook(${book.id}); modal.style.display='none';">
                    ✏️ Edit
                </button>
                <button class="btn btn-danger" onclick="deleteBook(${book.id}); modal.style.display='none';">
                    🗑️ Delete
                </button>
            </div>
        </div>
    `;

    modal.style.display = 'block';
}

// Handle form submission
async function handleFormSubmit(e) {
    e.preventDefault();

    const formData = new FormData();
    formData.append('title', titleInput.value);
    formData.append('description', descriptionInput.value);
    formData.append('price', priceInput.value);
    
    if (imageInput.files[0]) {
        formData.append('image', imageInput.files[0]);
    }

    try {
        const url = isEditMode 
            ? `${API_BASE_URL}/${bookIdInput.value}`
            : API_BASE_URL;
        
        const method = isEditMode ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            body: formData
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const book = await response.json();
        
        showSuccess(`Book ${isEditMode ? 'updated' : 'added'} successfully!`);
        resetForm();
        loadBooks();
    } catch (error) {
        console.error('Error submitting form:', error);
        showError(`Failed to ${isEditMode ? 'update' : 'add'} book. Please try again.`);
    }
}

// Edit book
async function editBook(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/${id}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const book = await response.json();
        
        // Populate form
        bookIdInput.value = book.id;
        titleInput.value = book.title;
        descriptionInput.value = book.description;
        priceInput.value = book.price;
        
        // Update UI
        isEditMode = true;
        formTitle.textContent = 'Edit Book';
        submitText.textContent = 'Update Book';
        cancelBtn.style.display = 'inline-block';
        
        // Scroll to form
        document.querySelector('.form-section').scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        console.error('Error loading book:', error);
        showError('Failed to load book details. Please try again.');
    }
}

// Delete book
async function deleteBook(id) {
    if (!confirm('Are you sure you want to delete this book?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        showSuccess('Book deleted successfully!');
        loadBooks();
    } catch (error) {
        console.error('Error deleting book:', error);
        showError('Failed to delete book. Please try again.');
    }
}

// Reset form
function resetForm() {
    bookForm.reset();
    bookIdInput.value = '';
    isEditMode = false;
    formTitle.textContent = 'Add New Book';
    submitText.textContent = 'Add Book';
    cancelBtn.style.display = 'none';
}

// Show error message
function showError(message) {
    errorMessageDiv.textContent = message;
    errorMessageDiv.style.display = 'block';
    setTimeout(() => {
        errorMessageDiv.style.display = 'none';
    }, 5000);
}

// Show success message
function showSuccess(message) {
    const successDiv = document.createElement('div');
    successDiv.className = 'success-message';
    successDiv.textContent = message;
    
    const formSection = document.querySelector('.form-section');
    formSection.insertBefore(successDiv, formSection.firstChild);
    
    setTimeout(() => {
        successDiv.remove();
    }, 3000);
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
