# Bookshelf Application - Google Cloud Storage Integration

This application now uses Google Cloud Storage to store book cover images instead of local file storage.

## Prerequisites

1. **Google Cloud Platform Account**
   - Create a GCP account at https://cloud.google.com
   - Create a new project or use an existing one

2. **Google Cloud Storage Bucket**
   - Create a Cloud Storage bucket in your GCP project
   - Note your bucket name for configuration

3. **Authentication**
   - Set up Google Cloud authentication using one of these methods:

### Option 1: Application Default Credentials (Recommended for Development)

```bash
# Install Google Cloud SDK
# Download from: https://cloud.google.com/sdk/docs/install

# Initialize and authenticate
gcloud init
gcloud auth application-default login
```

### Option 2: Service Account (Recommended for Production)

1. Create a service account in Google Cloud Console
2. Grant the service account "Storage Object Admin" role
3. Download the JSON key file
4. Set the environment variable or configure in application.properties

## Configuration

### Environment Variables

Set these environment variables before running the application:

```bash
# Windows PowerShell
$env:GCP_PROJECT_ID="your-project-id"
$env:GCS_BUCKET_NAME="your-bucket-name"

# Optional: If using service account JSON key
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\service-account-key.json"
```

```bash
# Linux/Mac
export GCP_PROJECT_ID="your-project-id"
export GCS_BUCKET_NAME="your-bucket-name"

# Optional: If using service account JSON key
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
```

### Alternative: Update application.properties

Edit `src/main/resources/application.properties`:

```properties
spring.cloud.gcp.project-id=your-project-id
spring.cloud.gcp.storage.bucket-name=your-bucket-name

# If using service account key file
spring.cloud.gcp.credentials.location=classpath:gcp-credentials.json
```

## Building and Running

```bash
# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/bookshelf-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Upload Book with Cover Image

```bash
POST /api/books
Content-Type: multipart/form-data

Fields:
- title: string (required)
- description: string (required)
- price: decimal (required)
- image: file (optional)
```

### Update Book with New Cover Image

```bash
PUT /api/books/{id}
Content-Type: multipart/form-data

Fields:
- title: string (required)
- description: string (required)
- price: decimal (required)
- image: file (optional)
```

### Get All Books

```bash
GET /api/books
```

Response includes `imageUrl` field with the full GCS public URL.

### Get Book by ID

```bash
GET /api/books/{id}
```

### Delete Book

```bash
DELETE /api/books/{id}
```

This will also delete the associated image from Cloud Storage.

## Image Storage Details

- Images are stored in Google Cloud Storage bucket
- Images are organized in the `book-covers/` folder
- Each image has a unique UUID filename
- Supported formats: jpg, jpeg, png, gif, bmp, webp
- Maximum file size: 10MB
- Images are publicly accessible via `imageUrl` in the response

## Security Best Practices

1. **Bucket Permissions**: Configure your GCS bucket with appropriate IAM policies
2. **CORS Configuration**: If accessing from a web frontend, configure CORS on your bucket:

```json
[
  {
    "origin": ["*"],
    "method": ["GET"],
    "responseHeader": ["Content-Type"],
    "maxAgeSeconds": 3600
  }
]
```

Apply CORS configuration:
```bash
gsutil cors set cors.json gs://your-bucket-name
```

3. **Private Images**: If you need private images, modify CloudStorageService to use signed URLs:

```java
// Generate a signed URL valid for 1 hour
String signedUrl = cloudStorageService.generateSignedUrl(blobName, 1, TimeUnit.HOURS);
```

## Troubleshooting

### Authentication Issues

**Error**: "Could not find Application Default Credentials"

**Solution**: Run `gcloud auth application-default login` or set `GOOGLE_APPLICATION_CREDENTIALS`

### Bucket Access Issues

**Error**: "403 Forbidden" when uploading

**Solution**: Ensure your service account or user has "Storage Object Admin" role on the bucket

### Dependencies Not Found

**Error**: Cannot resolve dependencies

**Solution**: Run `mvn clean install -U` to force update dependencies

## Cost Considerations

- Google Cloud Storage pricing: https://cloud.google.com/storage/pricing
- Free tier: 5GB-months of Regional Storage
- Operations: Class A (writes) and Class B (reads) operations have costs
- Network egress: Data transfer out of GCP may incur charges

## Migration from Local Storage

The application now uses Cloud Storage exclusively. To migrate existing local images:

1. Upload existing images from `uploads/` directory to your GCS bucket
2. Update database records to contain the full GCS URLs
3. Or use the update endpoint to re-upload images

## Testing

Run tests with:

```bash
mvn test
```

For integration testing with GCS, ensure you have proper authentication configured.
