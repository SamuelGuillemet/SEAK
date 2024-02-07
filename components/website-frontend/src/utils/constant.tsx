import { ListBulletIcon, PersonIcon } from "@radix-ui/react-icons";

import { SecurityScopes } from "@/openapi-codegen/apiSchemas";
import { pages } from "@/utils/pages";

export const BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL;

export const LOCAL_BACKEND_URL = process.env.LOCAL_BACKEND_URL;

export type LinksType = Array<{
  label: string;
  href: string;
  scopes: SecurityScopes[];
  icon?: React.ReactNode;
}>;

export const tabLinks: LinksType = [
  {
    label: "Mon compte",
    href: pages.account.index,
    scopes: ["user", "admin"],
    icon: <PersonIcon className="h-6 w-6" />,
  },
  {
    label: "Comptes utilisateurs",
    href: pages.account.users,
    scopes: ["admin"],
    icon: <ListBulletIcon className="h-6 w-6" />,
  },
];

export const navLinks: LinksType = [
  { label: "Classement", href: pages.index, scopes: ["user", "admin"] },
  { label: "Utilisateurs", href: pages.account.users, scopes: ["admin"] },
];
