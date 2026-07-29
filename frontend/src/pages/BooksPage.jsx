import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  checkBookAvailability,
  clearBookActionError,
  createBook,
  deleteBook,
  fetchBooks,
  updateBook,
} from "../features/books/booksSlice";

function formatDateTime(dateTime) {
  if (!dateTime) {
    return "-";
  }

  return new Date(dateTime).toLocaleString("en-IE", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatAvailability(availability){
  if (availability.available){
    return "Available";
  }

  if (availability.remainingDays>0){
    return `Not available, ${availability.remainingDays} day(s) remaining`;
  }

  if (availability.remainingDays ===0){
    return "Not available, due today";
  }

  return `Not available, overdue by ${Math.abs(availability.remainingDays)} day(s)`;
}

function BooksPage() {
  const dispatch = useDispatch();

  const books = useSelector((state) => state.books.items);
  const status = useSelector((state) => state.books.status);
  const error = useSelector((state) => state.books.error);

  const page = useSelector((state) => state.books.page);
  const size = useSelector((state) => state.books.size);
  const totalPages = useSelector((state) => state.books.totalPages);
  const totalElements = useSelector((state) => state.books.totalElements);

  const actionStatus = useSelector((state) => state.books.actionStatus);
  const actionError = useSelector((state) => state.books.actionError);

  const availabilityByBookId = useSelector(
    (state) => state.books.availabilityByBookId,
  );

  const [formData, setFormData] = useState({
    title: "",
    author: "",
    isbn: "",
    publishedYear: "",
  });

  const [search, setSearch] = useState("");

  const [editingBookId, setEditingBookId] = useState(null);
  const [editingTitle, setEditingTitle] = useState("");
  const [editingAuthor, setEditingAuthor] = useState("");

  useEffect(() => {
    if (status === "idle") {
      dispatch(
        fetchBooks({
          search: "",
          page: 0,
          size: 5,
        }),
      );
    }
  }, [status, dispatch]);

  function handleChange(event) {
    const { name, value } = event.target;

    setFormData({
      ...formData,
      [name]: value,
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const bookData = {
      title: formData.title,
      author: formData.author,
      isbn: formData.isbn,
      publishedYear:
        formData.publishedYear === "" ? null : Number(formData.publishedYear),
    };

    try {
      await dispatch(createBook(bookData)).unwrap();
      // unwrap: if backend responses successfully,

      setFormData({
        title: "",
        author: "",
        isbn: "",
        publishedYear: "",
      });

      dispatch(
        fetchBooks({
          search,
          page: 0,
          size,
        }),
      );
    } catch {
      // There is .addCase(createBook.rejected, xxx) in booksSlice already
      // so Redux has stored the error handling logic
    }
  }

  function handleSearch(event) {
    event.preventDefault();

    dispatch(clearBookActionError());

    dispatch(
      fetchBooks({
        search,
        page: 0,
        size,
      }),
    );
  }

  function handleClearSearch() {
    setSearch("");
    dispatch(clearBookActionError());

    dispatch(
      fetchBooks({
        search: "",
        page: 0,
        size,
      }),
    );
  }

  function handlePageChange(newPage) {
    dispatch(
      fetchBooks({
        search,
        page: newPage,
        size,
      }),
    );
  }

  function handleCheckAvailability(bookId) {
    dispatch(checkBookAvailability(bookId));
  }

  function startEditing(book) {
    dispatch(clearBookActionError());

    setEditingBookId(book.id);
    setEditingTitle(book.title);
    setEditingAuthor(book.author);
  }

  function cancelEditing() {
    setEditingBookId(null);
    setEditingTitle("");
    setEditingAuthor("");
  }

  async function handleUpdate(book) {
    try {
      await dispatch(
        updateBook({
          bookId: book.id,
          title: editingTitle,
          author: editingAuthor,
        }),
      ).unwrap();

      setEditingBookId(null);
      setEditingTitle("");
      setEditingAuthor("");
    } catch {}
  }

  async function handleDelete(bookId) {
    const confirmed = window.confirm(
      `Are you sure you want to delete book ${bookId}?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      await dispatch(deleteBook(bookId)).unwrap();

      const shouldGoToPreviousPage = books.length === 1 && page > 0;

      dispatch(
        fetchBooks({
          search,
          page: shouldGoToPreviousPage ? page - 1 : page,
          size,
        }),
      );
    } catch {}
  }

  return (
    <section>
      <h2>Books</h2>
      <p>View, add, update, delete, and check book availability.</p>

      <form className="form-card" onSubmit={handleSubmit}>
        <h3>Add New Book</h3>

        <p className="form-notice">
          ISBN and published year are permanent after creation.
          Please check them carefully before submitting.
        </p>

        <input
          name="title"
          placeholder="Title"
          value={formData.title}
          onChange={handleChange}
          required
        />

        <input
          name="author"
          placeholder="Author"
          value={formData.author}
          onChange={handleChange}
          required
        />

        <input
          name="isbn"
          placeholder="ISBN"
          value={formData.isbn}
          onChange={handleChange}
          required
        />

        <input
          name="publishedYear"
          placeholder="Published Year"
          value={formData.publishedYear}
          onChange={handleChange}
          type="number"
        />

        <button type="submit" disabled={actionStatus === "loading"}>
          {actionStatus === "loading" ? "Saving..." : "Add Book"}
        </button>
      </form>

      {actionError && <p className="error-message">{actionError}</p>}

      <form className="form-card" onSubmit={handleSearch}>
        <h3>Search Books</h3>

        <input
          placeholder="Search by title or author"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />

        <button type="submit">Search</button>

        <button type="button" onClick={handleClearSearch}>
          Clear
        </button>
      </form>

      {status === "loading" && <p>Loading books...</p>}

      {status === "failed" && <p className="error-message">{error}</p>}

      {status === "succeeded" && (
        <>
          <p>Total books: {totalElements}</p>

          {books.length === 0 ? (
            <p>No books were found.</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Author</th>
                  <th>ISBN</th>
                  <th>Published Year</th>
                  <th>Created At</th>
                  <th>Availability</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {books.map((book) => (
                  <tr key={book.id}>
                    <td>{book.id}</td>

                    <td>
                      {editingBookId === book.id ? (
                        <input
                          value={editingTitle}
                          onChange={(event) =>
                            setEditingTitle(event.target.value)
                          }
                          required
                        />
                      ) : (
                        book.title
                      )}
                    </td>

                    <td>
                      {editingBookId === book.id ? (
                        <input
                          value={editingAuthor}
                          onChange={(event) =>
                            setEditingAuthor(event.target.value)
                          }
                          required
                        />
                      ) : (
                        book.author
                      )}
                    </td>

                    <td>{book.isbn}</td>

                    <td>{book.publishedYear || "-"}</td>

                    <td>{formatDateTime(book.createdAt)}</td>

                    <td>
                      <button
                        type="button"
                        onClick={() => handleCheckAvailability(book.id)}
                      >
                        Check
                      </button>

                      {availabilityByBookId[book.id] && (
                        <span className="availability-text">
                          {formatAvailability(availabilityByBookId[book.id])}
                        </span>
                      )}
                    </td>

                    <td>
                      {editingBookId === book.id ? (
                        <>
                          <button
                            type="button"
                            onClick={() => handleUpdate(book)}
                            disabled={
                              actionStatus === "loading" ||
                              !editingTitle.trim() ||
                              !editingAuthor.trim()
                            }
                          >
                            Save{" "}
                          </button>
                          <button type="button" onClick={cancelEditing}>
                            Cancel
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            type="button"
                            onClick={() => startEditing(book)}
                          >
                            Edit
                          </button>

                          <button
                            type="button"
                            onClick={() => handleDelete(book.id)}
                          >
                            Delete
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {totalPages > 1 && (
            <div className="pagination">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => handlePageChange(page - 1)}
              >
                Previous
              </button>

              <span>
                Page {page + 1} of {totalPages}
              </span>

              <button
                type="button"
                disabled={page + 1 >= totalPages}
                onClick={() => handlePageChange(page + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
}

export default BooksPage;
