import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  beforeEach,
  describe,
  expect,
  test,
  vi,
} from "vitest";
import { useDispatch, useSelector } from "react-redux";
import UsersPage from "./UsersPage";

const {
  mockFetchUsers,
  mockCreateUser,
  mockUpdateUser,
  mockDeleteUser,
  mockClearUserActionError,
} = vi.hoisted(() => ({
  mockFetchUsers: vi.fn(),
  mockCreateUser: vi.fn(),
  mockUpdateUser: vi.fn(),
  mockDeleteUser: vi.fn(),
  mockClearUserActionError: vi.fn(),
}));

vi.mock("react-redux", () => ({
  useDispatch: vi.fn(),
  useSelector: vi.fn(),
}));

vi.mock("../features/users/usersSlice", () => ({
  fetchUsers: mockFetchUsers,
  createUser: mockCreateUser,
  updateUser: mockUpdateUser,
  deleteUser: mockDeleteUser,
  clearUserActionError: mockClearUserActionError,
}));

describe("UsersPage", () => {
  const mockDispatch = vi.fn();

  const user1 = {
    id: 1,
    name: "Alice",
    email: "alice@test.com",
    createdAt: "2026-07-01T10:00:00",
    updatedAt: null,
  };

  const user2 = {
    id: 2,
    name: "Bob",
    email: "bob@test.com",
    createdAt: "2026-07-02T10:00:00",
    updatedAt: null,
  };

  const mockState = {
    users: {
      items: [user1, user2],
      status: "succeeded",
      error: null,

      page: 0,
      size: 5,
      totalPages: 1,
      totalElements: 2,

      actionStatus: "idle",
      actionError: null,
    },
  };

  function setMockState(state) {
    useSelector.mockImplementation((selector) => selector(state));
  }

  beforeEach(() => {
    vi.clearAllMocks();

    useDispatch.mockReturnValue(mockDispatch);
    setMockState(mockState);

    mockClearUserActionError.mockReturnValue({
      type: "users/clearUserActionError",
    });
  });

  test("should fetch users on initial render", () => {
    const idleState = {
      users: {
        ...mockState.users,
        items: [],
        status: "idle",
        totalElements: 0,
      },
    };

    const fetchUsersAction = {
      type: "users/fetchUsers/pending",
    };

    setMockState(idleState);

    mockFetchUsers.mockReturnValue(fetchUsersAction);

    render(<UsersPage />);

    expect(mockFetchUsers).toHaveBeenCalledWith({
      search: "",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchUsersAction,
    );
  });

  test("should search users", async () => {
    const user = userEvent.setup();

    const fetchUsersAction = {
      type: "users/fetchUsers/pending",
    };

    mockFetchUsers.mockReturnValue(fetchUsersAction);

    render(<UsersPage />);

    await user.type(
      screen.getByPlaceholderText(
        "Search by name or email",
      ),
      "alice",
    );

    await user.click(
      screen.getByRole("button", {
        name: "Search",
      }),
    );

    expect(mockClearUserActionError).toHaveBeenCalled();

    expect(mockFetchUsers).toHaveBeenCalledWith({
      search: "alice",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchUsersAction,
    );
  });

  test("should create a user", async () => {
    const user = userEvent.setup();

    const createUserAction = {
      type: "users/createUser/pending",
    };

    const fetchUsersAction = {
      type: "users/fetchUsers/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({
      id: 3,
    });

    mockCreateUser.mockReturnValue(createUserAction);
    mockFetchUsers.mockReturnValue(fetchUsersAction);

    mockDispatch.mockImplementation((action) => {
      if (action === createUserAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<UsersPage />);

    await user.type(
      screen.getByPlaceholderText("Name"),
      "Charlie",
    );

    await user.type(
      screen.getByPlaceholderText("Email"),
      "charlie@test.com",
    );

    await user.click(
      screen.getByRole("button", {
        name: "Add User",
      }),
    );

    expect(mockCreateUser).toHaveBeenCalledWith({
      name: "Charlie",
      email: "charlie@test.com",
    });

    expect(unwrap).toHaveBeenCalled();

    expect(mockFetchUsers).toHaveBeenCalledWith({
      search: "",
      page: 0,
      size: 5,
    });
  });

  test("should edit a user", async () => {
    const user = userEvent.setup();

    const updateUserAction = {
      type: "users/updateUser/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({});

    mockUpdateUser.mockReturnValue(updateUserAction);

    mockDispatch.mockImplementation((action) => {
      if (action === updateUserAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<UsersPage />);

    await user.click(
      screen.getAllByRole("button", {
        name: "Edit",
      })[0],
    );

    const input = screen.getByDisplayValue("Alice");

    await user.clear(input);

    await user.type(input, "Alice Smith");

    await user.click(
      screen.getByRole("button", {
        name: "Save",
      }),
    );

    expect(mockUpdateUser).toHaveBeenCalledWith({
      userId: 1,
      name: "Alice Smith",
    });

    expect(unwrap).toHaveBeenCalled();
  });
    test("should delete a user", async () => {
    const user = userEvent.setup();

    vi.spyOn(window, "confirm").mockReturnValue(true);

    const deleteUserAction = {
      type: "users/deleteUser/pending",
    };

    const fetchUsersAction = {
      type: "users/fetchUsers/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({});

    mockDeleteUser.mockReturnValue(deleteUserAction);
    mockFetchUsers.mockReturnValue(fetchUsersAction);

    mockDispatch.mockImplementation((action) => {
      if (action === deleteUserAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<UsersPage />);

    await user.click(
      screen.getAllByRole("button", {
        name: "Delete",
      })[0],
    );

    expect(window.confirm).toHaveBeenCalledWith(
      "Are you sure you want to delete user 1?",
    );

    expect(mockDeleteUser).toHaveBeenCalledWith(1);

    expect(unwrap).toHaveBeenCalled();

    expect(mockFetchUsers).toHaveBeenCalledWith({
      search: "",
      page: 0,
      size: 5,
    });
  });

  test("should fetch the next page", async () => {
    const user = userEvent.setup();

    const pagedState = {
      users: {
        ...mockState.users,
        page: 0,
        totalPages: 3,
        totalElements: 12,
      },
    };

    const fetchUsersAction = {
      type: "users/fetchUsers/pending",
    };

    setMockState(pagedState);

    mockFetchUsers.mockReturnValue(fetchUsersAction);

    render(<UsersPage />);

    await user.click(
      screen.getByRole("button", {
        name: "Next",
      }),
    );

    expect(mockFetchUsers).toHaveBeenCalledWith({
      search: "",
      page: 1,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchUsersAction,
    );
  });

  test("should display users", () => {
    render(<UsersPage />);

    expect(
      screen.getByRole("heading", {
        name: "Users",
      }),
    ).toBeInTheDocument();

    expect(
      screen.getByText("Total users: 2"),
    ).toBeInTheDocument();

    expect(screen.getByText("Alice")).toBeInTheDocument();

    expect(
      screen.getByText("alice@test.com"),
    ).toBeInTheDocument();

    expect(screen.getByText("Bob")).toBeInTheDocument();

    expect(
      screen.getByText("bob@test.com"),
    ).toBeInTheDocument();
  });

  test("should display loading message", () => {
    const loadingState = {
      users: {
        ...mockState.users,
        items: [],
        status: "loading",
      },
    };

    setMockState(loadingState);

    render(<UsersPage />);

    expect(
      screen.getByText("Loading users..."),
    ).toBeInTheDocument();
  });

  test("should display loading error", () => {
    const failedState = {
      users: {
        ...mockState.users,
        items: [],
        status: "failed",
        error: "Unable to load users",
      },
    };

    setMockState(failedState);

    render(<UsersPage />);

    expect(
      screen.getByText("Unable to load users"),
    ).toBeInTheDocument();
  });

  test("should display action error", () => {
    const actionErrorState = {
      users: {
        ...mockState.users,
        actionError: "Unable to create user",
      },
    };

    setMockState(actionErrorState);

    render(<UsersPage />);

    expect(
      screen.getByText("Unable to create user"),
    ).toBeInTheDocument();
  });

  test("should display empty message", () => {
    const emptyState = {
      users: {
        ...mockState.users,
        items: [],
        totalElements: 0,
      },
    };

    setMockState(emptyState);

    render(<UsersPage />);

    expect(
      screen.getByText("No users were found."),
    ).toBeInTheDocument();
  });
});