import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  clearUserActionError,
  createUser,
  deleteUser,
  fetchUsers,
  updateUser,
} from "../features/users/usersSlice";

function UsersPage() {
  const dispatch = useDispatch();
  const users = useSelector((state) => state.users.items);
  const status = useSelector((state) => state.users.status);
  const error = useSelector((state) => state.users.error);

  const page = useSelector((state) => state.users.page);
  const size = useSelector((state) => state.users.size);
  const totalPages = useSelector((state) => state.users.totalPages);
  const totalElements = useSelector((state) => state.users.totalElements);

  const actionStatus = useSelector((state) => state.users.actionStatus);
  const actionError = useSelector((state) => state.users.actionError);

  const [formData, setFormData] = useState({
    name: "",
    email: "",
  });

  const [search, setSearch] = useState("");

  const [editingUserId, setEditingUserId] = useState(null);
  const [editingName, setEditingName] = useState("");

  useEffect(() => {
    if (status === "idle") {
      dispatch(
        fetchUsers({
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

    try {
      await dispatch(
        createUser({
          name: formData.name,
          email: formData.email,
        }),
      ).unwrap();

      // clean the form inputs
      setFormData({
        name: "",
        email: "",
      });

      // re-search the users list
      dispatch(
        fetchUsers({
          search,
          page: 0,
          size,
        }),
      );
    } catch {}
  }

  function handleSearch(event) {
    event.preventDefault();

    dispatch(clearUserActionError());

    dispatch(
      fetchUsers({
        search,
        page: 0,
        size,
      }),
    );
  }

  function handleClearSearch() {
    setSearch("");

    dispatch(
      fetchUsers({
        search: "",
        page: 0,
        size,
      }),
    );
  }

  function handlePageChange(newPage) {
    dispatch(
      fetchUsers({
        search,
        page: newPage,
        size,
      }),
    );
  }

  function startEditing(user) {
    dispatch(clearUserActionError());

    setEditingUserId(user.id);
    setEditingName(user.name);
  }

  function cancelEditing() {
    setEditingUserId(null);
    setEditingName("");
  }

  async function handleUpdate(userId) {
    try {
      await dispatch(
        updateUser({
          userId,
          name: editingName,
        }),
      ).unwrap();

      setEditingUserId(null);
      setEditingName("");
    } catch {}
  }

  async function handleDelete(userId) {
    const confirmed = window.confirm(
      `Are you sure you want to delete user ${userId}?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      await dispatch(deleteUser(userId)).unwrap();

      const shouldGoToPreviousPage = users.length === 1 && page > 0;

      dispatch(
        fetchUsers({
          search,
          page: shouldGoToPreviousPage ? page - 1 : page,
          size,
        }),
      );
    } catch {}
  }

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

  return (
    <section>
      <h2>Users</h2>
      <p>View, add, update, and soft delete users.</p>

      <form className="form-card" onSubmit={handleSubmit}>
        <h3>Add New User</h3>

        <input
          name="name"
          placeholder="Name"
          value={formData.name}
          onChange={handleChange}
          required
        />

        <input
          name="email"
          placeholder="Email"
          value={formData.email}
          onChange={handleChange}
          required
          type="email"
        />

        <button type="submit" disabled={actionStatus === "loading"}>
          {actionStatus === "loading" ? "Saving..." : "Add User"}
        </button>
      </form>

      {actionError && <p className="error-message">{actionError}</p>}

      <form className="form-card" onSubmit={handleSearch}>
        <h3>Search Users</h3>

        <input
          placeholder="Search by name or email"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />

        <button type="submit">Search</button>

        <button type="button" onClick={handleClearSearch}>
          Clear
        </button>
      </form>

      {status === "loading" && <p>Loading users...</p>}

      {status === "failed" && <p className="error-message">{error}</p>}

      {status === "succeeded" && (
        <>
          <p>Total users: {totalElements}</p>

          {users.length === 0 ? (
            <p>No users were found.</p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Created At</th>
                  <th>Updated At</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>

                    <td>
                      {editingUserId === user.id ? (
                        <input
                          value={editingName}
                          onChange={(event) =>
                            setEditingName(event.target.value)
                          }
                          required
                        />
                      ) : (
                        user.name
                      )}
                    </td>

                    <td>{user.email}</td>

                    <td>{formatDateTime(user.createdAt)}</td>

                    <td>{formatDateTime(user.updatedAt)}</td>

                    <td>
                      {editingUserId === user.id ? (
                        <>
                          <button
                            type="button"
                            onClick={() => handleUpdate(user.id)}
                            disabled={
                              actionStatus === "loading" || !editingName.trim()
                            }
                          >
                            Save
                          </button>

                          <button type="button" onClick={cancelEditing}>
                            Cancel
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            type="button"
                            onClick={() => startEditing(user)}
                          >
                            Edit
                          </button>

                          <button
                            type="button"
                            onClick={() => handleDelete(user.id)}
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

export default UsersPage;
